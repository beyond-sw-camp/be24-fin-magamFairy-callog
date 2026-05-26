package org.example.backend.campaign.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignDto;
import org.example.backend.campaign.model.CampaignMember;
import org.example.backend.campaign.model.CampaignMemberRole;
import org.example.backend.campaign.model.CampaignParticipant;
import org.example.backend.campaign.model.CampaignRole;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.campaign.repository.CampaignParticipantRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.common.redis.CacheNames;
import org.example.backend.common.redis.DashboardCacheEvictor;
import org.example.backend.kpi.service.CampaignKpiContributionService;
import org.example.backend.notification.service.NotificationSseService;
import org.example.backend.organization.model.OrganizationType;
import org.example.backend.teamboard.repository.TaskRepository;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignService {
    private static final String DEFAULT_COLOR = "#8B5CF6";
    private static final String DEFAULT_ICON = "🎯";
    private static final List<String> ALLOWED_STATUSES = List.of(
            "draft", "in_progress", "recruiting", "review", "completed", "closed",
            // 하위 호환 — 기존 데이터에 저장된 구 값 허용
            "live", "paused", "at_risk", "planned", "active"
    );

    /**
     * 캠페인 생성 시 색상이 비어있을 때 무작위로 부여하는 팔레트 (20색).
     * 호버 그라디언트(#xxxxxxdd / #xxxxxx66 알파 합성)와 어울리는 미디엄 채도/명도 위주.
     */
    private static final List<String> CAMPAIGN_PALETTE = List.of(
            "#7C3AED", // 01 Electric Violet     — Tailwind violet-600, 2025 SaaS 표준
            "#B68BD9", // 02 Iris Bloom          — WGSN 2026 Lavender
            "#4F4878", // 03 Future Dusk         — WGSN/Coloro 2026 시즌 헤드
            "#4A4E97", // 04 Aura Indigo         — WGSN 2026 Indigo-Violet
            "#2740B2", // 05 Galactic Cobalt     — WGSN 2026 Deep Cobalt
            "#0EA5E9", // 06 Sky Cobalt          — Tailwind sky-500
            "#14B8A6", // 07 Mint Fresh          — Tailwind teal-500
            "#BEF264", // 08 Cyber Lime          — 2025 Neo-Chartreuse
            "#F59E0B", // 09 Honey Amber         — Tailwind amber-500
            "#FFBE98", // 10 Peach Fuzz          — Pantone COY 2024
            "#FF6B45", // 11 Lush Lava           — WGSN 2026 Vivid Orange-Red
            "#FB7185", // 12 Coral Reef          — Tailwind rose-400
            "#C5174D", // 13 Sour Cherry         — WGSN 2026 Bold Red
            "#EC4899", // 14 Petal Pink          — Tailwind pink-500
            "#A47864", // 15 Mocha Mousse        — Pantone COY 2025
            "#2F5D3D", // 16 Forest Sage         — 2025–26 Sustainability
            "#FCD34D", // 17 Buttercream         — 2025 Soft Yellow
            "#67E8F9", // 18 Glacier Mint        — 2025 Cool Refresh Cyan
            "#475569", // 19 Storm Slate         — Linear/Vercel 모던 뉴트럴
            "#D946EF"  // 20 Hyperreal Magenta   — 2025 Y2K Vivid
    );

    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final CampaignParticipantRepository participantRepository;
    private final CampaignMemberRepository memberRepository;
    private final CampaignKpiContributionService contributionService;
    private final NotificationSseService sseService;
    private final TaskRepository taskRepository;
    private final DashboardCacheEvictor dashboardCacheEvictor;

    /**
     * scope = "mine" → 내가 멤버인 캠페인 (CampaignMember 기준)
     * scope = "org"  → 내 조직이 참여하는 캠페인 (CampaignParticipant 기준)
     * Campaign.tags / partners 는 @BatchSize(50) 으로 배치 로딩되어 N+1 회피.
     */
    @Cacheable(value = CacheNames.CAMPAIGN_LIST, key = "#userIdx + ':' + #scope")
    public List<CampaignDto.Res> listCampaigns(Long userIdx, String scope) {
        User user = userRepository.findById(userIdx).orElse(null);
        List<Campaign> campaigns;
        if ("org".equalsIgnoreCase(scope) && user != null && user.getOrganization() != null) {
            Long orgIdx = user.getOrganization().getIdx();
            campaigns = participantRepository.findCampaignsByOrganizationIdx(orgIdx);
        } else {
            // 기본 — mine
            campaigns = memberRepository.findAllWithCampaignByUserIdx(userIdx).stream()
                    .map(CampaignMember::getCampaign)
                    .toList();
        }
        Map<Long, Integer> taskCounts = loadTaskCountMap(
                campaigns.stream().map(Campaign::getIdx).toList());
        return campaigns.stream()
                .map(c -> buildResponseFor(c, user, taskCounts.getOrDefault(c.getIdx(), 0)))
                .toList();
    }

    /**
     * 캠페인 idx 목록에 대해 한 번의 GROUP BY 쿼리로 task 수 조회.
     * Dashboard / CampaignList 의 N+1 회피용.
     */
    private Map<Long, Integer> loadTaskCountMap(List<Long> campaignIds) {
        if (campaignIds == null || campaignIds.isEmpty()) return Map.of();
        Map<Long, Integer> out = new HashMap<>();
        for (Object[] row : taskRepository.countByCampaignIdxIn(campaignIds)) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) continue;
            out.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return out;
    }

    @Transactional
    public CampaignDto.Res createCampaign(String ownerLoginId, CampaignDto.UpsertReq dto) {
        User owner = userRepository.findUserById(ownerLoginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));

        if (owner.getOrganization() != null
                && owner.getOrganization().getType() == OrganizationType.EXTERNAL_PARTNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "외부 파트너는 캠페인을 생성할 수 없습니다.");
        }

        String name = normalizeRequired(dto.name(), "Campaign name is required.");

        Campaign campaign = Campaign.builder()
                .ownerLoginId(ownerLoginId)
                .name(name)
                .purpose(normalizeText(dto.purpose()))
                .tags(normalizeList(dto.tags()))
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .partners(normalizeList(dto.partners()))
                .goals(normalizeText(dto.goals()))
                .assetName(normalizeText(dto.assetName()))
                .assetDescription(normalizeText(dto.assetDescription()))
                .primaryGoal(normalizeText(dto.primaryGoal()))
                .campaignMethods(normalizeList(dto.campaignMethods()))
                .maxCost(normalizeText(dto.maxCost()))
                .minRevenue(normalizeText(dto.minRevenue()))
                .status("draft")
                .initials(createInitials(name))
                .icon(normalizeIcon(dto.icon()))
                .color(pickInitialColor(dto.color()))
                .build();

        Campaign saved = campaignRepository.save(campaign);

        // owner를 GENERAL_MANAGER 멤버 + 그 조직을 PM으로 자동 등록
        if (owner.getOrganization() != null) {
            CampaignParticipant pmParticipant = CampaignParticipant.builder()
                    .campaign(saved)
                    .organization(owner.getOrganization())
                    .campaignRole(CampaignRole.PM)
                    .build();
            participantRepository.save(pmParticipant);
        }
        CampaignMember ownerMember = CampaignMember.builder()
                .campaign(saved)
                .user(owner)
                .campaignRole(CampaignMemberRole.GENERAL_MANAGER)
                .joinedAt(LocalDateTime.now())
                .build();
        memberRepository.save(ownerMember);

        // 모달에서 선택한 팀원(ownerUserIdxs) — 본인 제외하고 MANAGER로 자동 등록
        if (dto.ownerUserIdxs() != null && !dto.ownerUserIdxs().isEmpty()) {
            for (Long candidateIdx : dto.ownerUserIdxs()) {
                if (candidateIdx == null || candidateIdx.equals(owner.getIdx())) continue;
                User candidate = userRepository.findById(candidateIdx).orElse(null);
                if (candidate == null) continue;
                boolean alreadyMember = memberRepository
                        .findByCampaignIdxAndUserIdx(saved.getIdx(), candidate.getIdx())
                        .isPresent();
                if (alreadyMember) continue;
                memberRepository.save(CampaignMember.builder()
                        .campaign(saved)
                        .user(candidate)
                        .campaignRole(CampaignMemberRole.MANAGER)
                        .joinedAt(LocalDateTime.now())
                        .build());
            }
        }

        // KPI cascade: 캠페인 생성 시 상위 OrganizationKpi에 contribution 등록 (옵션)
        if (dto.contributions() != null && !dto.contributions().isEmpty()) {
            contributionService.bulkCreate(saved, dto.contributions());
        }

        // Dashboard 캐시 무효화 (active count, partnerCount, kpiCategories, blockers 영향)
        dashboardCacheEvictor.evictCampaign(saved.getIdx());

        return buildResponseFor(saved, owner);
    }

    @Transactional
    public CampaignDto.Res updateCampaign(String ownerLoginId, Long campaignId, CampaignDto.UpsertReq dto) {
        Campaign campaign = getEditableCampaign(ownerLoginId, campaignId);
        User user = userRepository.findUserById(ownerLoginId).orElse(null);
        String name = normalizeRequired(dto.name(), "Campaign name is required.");

        campaign.updateDetails(
                name,
                normalizeTextOrCurrent(dto.purpose(), campaign.getPurpose()),
                normalizeListOrCurrent(dto.tags(), campaign.getTags()),
                dto.startDate() == null ? campaign.getStartDate() : dto.startDate(),
                dto.endDate() == null ? campaign.getEndDate() : dto.endDate(),
                normalizeListOrCurrent(dto.partners(), campaign.getPartners()),
                normalizeTextOrCurrent(dto.goals(), campaign.getGoals()),
                normalizeTextOrCurrent(dto.assetName(), campaign.getAssetName()),
                normalizeTextOrCurrent(dto.assetDescription(), campaign.getAssetDescription()),
                normalizeTextOrCurrent(dto.primaryGoal(), campaign.getPrimaryGoal()),
                normalizeListOrCurrent(dto.campaignMethods(), campaign.getCampaignMethods()),
                normalizeTextOrCurrent(dto.maxCost(), campaign.getMaxCost()),
                normalizeTextOrCurrent(dto.minRevenue(), campaign.getMinRevenue()),
                createInitials(name),
                normalizeIcon(dto.icon()),
                normalizeColor(dto.color())
        );

        sseService.broadcastCalendarRefresh(campaign.getIdx(), "campaign");
        // Dashboard 캐시 무효화 (캠페인명/partner 변경 시 partnerProgress 등 stale 방지)
        dashboardCacheEvictor.evictCampaign(campaign.getIdx());
        return buildResponseFor(campaign, user);
    }

    @Transactional
    public CampaignDto.Res invitePartners(String ownerLoginId, Long campaignId, CampaignDto.PartnerInviteReq dto) {
        Campaign campaign = getEditableCampaign(ownerLoginId, campaignId);
        User user = userRepository.findUserById(ownerLoginId).orElse(null);
        campaign.updatePartners(normalizeList(dto.partners()));
        // Dashboard 캐시 무효화 (partnerCount, partnerProgress 영향)
        dashboardCacheEvictor.evictCampaign(campaign.getIdx());
        return buildResponseFor(campaign, user);
    }

    @Transactional
    public CampaignDto.Res updateStatus(String ownerLoginId, Long campaignId, CampaignDto.StatusReq dto) {
        Campaign campaign = getEditableCampaign(ownerLoginId, campaignId);
        User user = userRepository.findUserById(ownerLoginId).orElse(null);
        String status = normalizeRequired(dto.status(), "Campaign status is required.");

        if (!ALLOWED_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported campaign status.");
        }

        campaign.updateStatus(status);
        // Dashboard 캐시 무효화 (active count, activeByOrg 영향)
        dashboardCacheEvictor.evictCampaign(campaign.getIdx());
        return buildResponseFor(campaign, user);
    }

    /**
     * 응답 DTO에 호출 유저 기준 권한 정보(내 캠페인 역할, 내 조직이 PM인지)를 채워서 반환한다.
     * user가 null이면 권한 정보는 null/false로 채움.
     * 단일 캠페인 응답 (create/update 등) — task count 는 별도 쿼리로 채워 N+1 회피.
     */
    private CampaignDto.Res buildResponseFor(Campaign campaign, User user) {
        int taskCount = loadTaskCountMap(List.of(campaign.getIdx()))
                .getOrDefault(campaign.getIdx(), 0);
        return buildResponseFor(campaign, user, taskCount);
    }

    /**
     * task count 가 외부에서 미리 계산된 경우 (listCampaigns 의 group-by 결과) 그대로 주입.
     */
    private CampaignDto.Res buildResponseFor(Campaign campaign, User user, Integer totalTaskCount) {
        Long orgIdx = (user != null && user.getOrganization() != null) ? user.getOrganization().getIdx() : null;
        boolean isPmOrg = orgIdx != null && participantRepository.existsByCampaignIdxAndOrganizationIdxAndCampaignRole(
                campaign.getIdx(), orgIdx, CampaignRole.PM);
        CampaignMemberRole myRole = (user == null) ? null : memberRepository
                .findByCampaignIdxAndUserIdx(campaign.getIdx(), user.getIdx())
                .map(CampaignMember::getCampaignRole)
                .orElse(null);
        return CampaignDto.Res.from(campaign, myRole, isPmOrg, totalTaskCount);
    }

    /**
     * 캠페인 편집(수정/상태변경/초대) 권한을 가드하고 해당 캠페인 엔티티를 반환한다.
     * 두 조건을 모두 만족해야 통과:
     *   1) 내 조직이 이 캠페인의 PM (CampaignParticipant.campaignRole == PM)
     *   2) 내 캠페인 멤버 역할이 MANAGER 또는 GENERAL_MANAGER
     */
    private Campaign getEditableCampaign(String userLoginId, Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign was not found."));

        User user = userRepository.findUserById(userLoginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));

        if (user.getOrganization() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "조직 정보가 없는 계정입니다.");
        }

        boolean isPmOrg = participantRepository.existsByCampaignIdxAndOrganizationIdxAndCampaignRole(
                campaignId, user.getOrganization().getIdx(), CampaignRole.PM);

        CampaignMemberRole myRole = memberRepository
                .findByCampaignIdxAndUserIdx(campaignId, user.getIdx())
                .map(CampaignMember::getCampaignRole)
                .orElse(null);
        boolean isManagerOrGm = myRole == CampaignMemberRole.MANAGER
                || myRole == CampaignMemberRole.GENERAL_MANAGER;

        if (!isPmOrg || !isManagerOrGm) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "캠페인을 편집할 권한이 없습니다.");
        }

        return campaign;
    }

    private static String normalizeRequired(String value, String message) {
        String normalized = normalizeText(value);

        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        return normalized;
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }

        return new ArrayList<>(values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList());
    }

    private static String normalizeTextOrCurrent(String value, String current) {
        return value == null ? normalizeText(current) : normalizeText(value);
    }

    private static List<String> normalizeListOrCurrent(List<String> values, List<String> current) {
        return values == null ? normalizeList(current) : normalizeList(values);
    }

    /**
     * 캠페인 생성 전용 색상 결정.
     * 사용자가 명시한 색이 있으면 그대로, 비어있으면 CAMPAIGN_PALETTE에서 무작위 선택.
     */
    private static String pickInitialColor(String requested) {
        String normalized = normalizeText(requested);
        if (!normalized.isBlank()) {
            return normalized;
        }
        int index = ThreadLocalRandom.current().nextInt(CAMPAIGN_PALETTE.size());
        return CAMPAIGN_PALETTE.get(index);
    }

    /**
     * 캠페인 수정 전용 색상 정규화.
     * 사용자가 명시한 색이 있으면 그대로, 비어있으면 DEFAULT_COLOR 폴백.
     * (수정 시에는 랜덤 부여 금지 — 호출 때마다 색이 바뀌면 안 됨)
     */
    private static String normalizeColor(String color) {
        String normalized = normalizeText(color);
        return normalized.isBlank() ? DEFAULT_COLOR : normalized;
    }

    private static String normalizeIcon(String icon) {
        String normalized = normalizeText(icon);
        return normalized.isBlank() ? DEFAULT_ICON : normalized;
    }

    private static String createInitials(String name) {
        String normalized = normalizeText(name);

        if (normalized.isBlank()) {
            return "CP";
        }

        String[] words = normalized.split("\\s+");
        String initials = words.length > 1
                ? words[0].substring(0, 1) + words[1].substring(0, 1)
                : normalized.substring(0, Math.min(2, normalized.length()));

        return initials.toUpperCase();
    }
}
