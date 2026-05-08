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
import org.example.backend.kpi.service.CampaignKpiContributionService;
import org.example.backend.organization.model.OrganizationType;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignService {
    private static final String DEFAULT_COLOR = "#8B5CF6";
    private static final String DEFAULT_ICON = "🎯";
    private static final List<String> ALLOWED_STATUSES = List.of("draft", "review", "live", "paused", "completed");

    /**
     * 캠페인 생성 시 색상이 비어있을 때 무작위로 부여하는 팔레트 (20색).
     * 호버 그라디언트(#xxxxxxdd / #xxxxxx66 알파 합성)와 어울리는 미디엄 채도/명도 위주.
     */
    private static final List<String> CAMPAIGN_PALETTE = List.of(
            "#8B5CF6", // violet
            "#EC4899", // pink
            "#F59E0B", // amber
            "#10B981", // emerald
            "#3B82F6", // blue
            "#EF4444", // red
            "#06B6D4", // cyan
            "#84CC16", // lime
            "#F97316", // orange
            "#14B8A6", // teal
            "#6366F1", // indigo
            "#A855F7", // purple
            "#D946EF", // fuchsia
            "#F43F5E", // rose
            "#EAB308", // yellow
            "#22C55E", // green
            "#0EA5E9", // sky
            "#FB7185", // rose-light
            "#4F46E5", // indigo-deep
            "#059669"  // emerald-deep
    );

    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final CampaignParticipantRepository participantRepository;
    private final CampaignMemberRepository memberRepository;
    private final CampaignKpiContributionService contributionService;
    private final CampaignImageStorageService thumbnailStorage;
    private final CampaignThumbnailGenerator thumbnailGenerator;
    private final org.example.backend.matching.repository.BenefitRepository benefitRepository;

    public List<CampaignDto.Res> listCampaigns(Long userIdx) {
        return listCampaigns(userIdx, "mine");
    }

    /**
     * 캠페인 디렉토리 — `/campaigns/browse` 페이지용 검색·필터·태그 응답.
     *
     * Visibility:
     *  - HQ: 전체 캠페인
     *  - AFFILIATE: 본사 PM 또는 자기 조직 PM 또는 자기 조직 참여 캠페인
     *  - EXTERNAL_PARTNER: 자기 조직 참여 캠페인
     *  - 비로그인: 401 (controller에서 차단)
     */
    public List<org.example.backend.campaign.dto.CampaignDirectoryDto> directory(
            Long userIdx, String search, String orgType, String status, java.util.List<String> tags, String sort, String scope) {
        User caller = userRepository.findById(userIdx).orElse(null);

        java.util.List<Campaign> visible = collectVisibleForDirectory(caller);

        // scope = "mine" | "applied" | (default: 전체 visible)
        if ("mine".equalsIgnoreCase(scope) && caller != null) {
            String callerLoginId = caller.getId();
            visible = visible.stream()
                    .filter(c -> callerLoginId != null && callerLoginId.equals(c.getOwnerLoginId()))
                    .toList();
        } else if ("applied".equalsIgnoreCase(scope) && caller != null && caller.getOrganization() != null) {
            Long callerOrgIdx = caller.getOrganization().getIdx();
            // 자기 조직이 PartnerBenefits(=제안서 제출)을 등록한 캠페인 — 본사 승인 여부와 무관
            java.util.Set<Long> appliedIds = new java.util.HashSet<>(
                    benefitRepository.findCampaignIdxByOrganizationIdx(callerOrgIdx));
            visible = visible.stream()
                    .filter(c -> appliedIds.contains(c.getIdx()))
                    .toList();
        }

        // PM 조직 매핑 — 캠페인별 한 번만 lookup
        java.util.Map<Long, org.example.backend.organization.model.Organization> pmByCampaign = new java.util.HashMap<>();
        for (Campaign c : visible) {
            participantRepository.findAllByCampaignIdx(c.getIdx()).stream()
                    .filter(p -> p.getCampaignRole() == CampaignRole.PM && p.getOrganization() != null)
                    .findFirst()
                    .ifPresent(p -> pmByCampaign.put(c.getIdx(), p.getOrganization()));
        }

        String q = search == null ? "" : search.trim().toLowerCase();
        String statusLower = status == null ? "" : status.trim().toLowerCase();

        return visible.stream()
                .filter(c -> {
                    if (!q.isEmpty()) {
                        String hay = ((c.getName() == null ? "" : c.getName()) + " "
                                + (c.getPurpose() == null ? "" : c.getPurpose()) + " "
                                + (c.getMainMessage() == null ? "" : c.getMainMessage())).toLowerCase();
                        if (!hay.contains(q)) return false;
                    }
                    if (!statusLower.isEmpty() && !statusLower.equalsIgnoreCase(c.getStatus())) return false;
                    if (orgType != null && !orgType.isBlank() && !"ALL".equalsIgnoreCase(orgType)) {
                        var pm = pmByCampaign.get(c.getIdx());
                        if (pm == null || pm.getType() == null) return false;
                        if (!pm.getType().name().equalsIgnoreCase(orgType)) return false;
                    }
                    if (tags != null && !tags.isEmpty()) {
                        java.util.Set<String> cTags = c.getTags() == null ? java.util.Set.of()
                                : new java.util.HashSet<>(c.getTags());
                        if (tags.stream().noneMatch(cTags::contains)) return false;
                    }
                    return true;
                })
                .sorted((a, b) -> {
                    if ("deadline".equalsIgnoreCase(sort)) {
                        java.time.LocalDate ad = a.getEndDate(), bd = b.getEndDate();
                        if (ad == null && bd == null) return 0;
                        if (ad == null) return 1;
                        if (bd == null) return -1;
                        return ad.compareTo(bd);
                    }
                    // latest — createdAt desc
                    java.util.Date at = a.getCreatedAt() == null ? new java.util.Date(0) : a.getCreatedAt();
                    java.util.Date bt = b.getCreatedAt() == null ? new java.util.Date(0) : b.getCreatedAt();
                    return bt.compareTo(at);
                })
                .map(c -> org.example.backend.campaign.dto.CampaignDirectoryDto.from(
                        c,
                        pmByCampaign.get(c.getIdx()),
                        thumbnailStorage.createViewUrl(c.getThumbnailObjectKey())))
                .toList();
    }

    // ── 썸네일 업로드 (Phase 3) ─────────────────────────────

    /** 사용자가 직접 업로드 — presigned PUT URL 발급. */
    public CampaignImageStorageService.UploadUrlResult createThumbnailUploadUrl(
            String ownerLoginId, Long campaignId, String contentType, Long fileSize) {
        Campaign campaign = getEditableCampaign(ownerLoginId, campaignId);
        return thumbnailStorage.createUploadUrl(campaign.getIdx(), contentType, fileSize);
    }

    /** 업로드 완료 확인 → Campaign.thumbnailObjectKey 저장 (이전 키 있으면 삭제). */
    @Transactional
    public void confirmThumbnail(String ownerLoginId, Long campaignId, String objectKey) {
        Campaign campaign = getEditableCampaign(ownerLoginId, campaignId);
        if (objectKey == null || objectKey.isBlank()
                || !thumbnailStorage.isCampaignThumbKey(campaign.getIdx(), objectKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid thumbnail object key.");
        }
        thumbnailStorage.validateUploadedObject(objectKey);

        String previous = campaign.getThumbnailObjectKey();
        campaign.updateThumbnailObjectKey(objectKey);
        // 이전 썸네일 정리 (best-effort)
        if (previous != null && !previous.equals(objectKey)) {
            try { thumbnailStorage.deleteObject(previous); } catch (Exception ignored) { }
        }
    }

    /** 썸네일 삭제. */
    @Transactional
    public void clearThumbnail(String ownerLoginId, Long campaignId) {
        Campaign campaign = getEditableCampaign(ownerLoginId, campaignId);
        String previous = campaign.getThumbnailObjectKey();
        campaign.updateThumbnailObjectKey(null);
        if (previous != null) {
            try { thumbnailStorage.deleteObject(previous); } catch (Exception ignored) { }
        }
    }

    private java.util.List<Campaign> collectVisibleForDirectory(User caller) {
        if (caller == null || caller.getOrganization() == null) {
            return java.util.List.of();
        }
        OrganizationType type = caller.getOrganization().getType();
        Long callerOrgIdx = caller.getOrganization().getIdx();
        if (type == OrganizationType.HQ) {
            return campaignRepository.findAll();
        }
        // AFFILIATE / EXTERNAL_PARTNER — 자기 조직 참여 캠페인 + 자기 조직 제안서 제출 캠페인
        // (AFFILIATE는 본사 PM 캠페인 추가 노출)
        java.util.Set<Long> ids = new java.util.HashSet<>();
        java.util.List<Campaign> visible = new java.util.ArrayList<>();
        participantRepository.findCampaignsByOrganizationIdx(callerOrgIdx).forEach(c -> {
            if (ids.add(c.getIdx())) visible.add(c);
        });
        // 제안서 제출 이력만 있는 캠페인도 노출 (applied 탭에서 확인 가능)
        java.util.List<Long> appliedIds = benefitRepository.findCampaignIdxByOrganizationIdx(callerOrgIdx);
        if (!appliedIds.isEmpty()) {
            campaignRepository.findAllById(appliedIds).forEach(c -> {
                if (ids.add(c.getIdx())) visible.add(c);
            });
        }
        if (type == OrganizationType.AFFILIATE) {
            campaignRepository.findAll().forEach(c -> {
                boolean hqPm = participantRepository.findAllByCampaignIdx(c.getIdx()).stream()
                        .anyMatch(p -> p.getCampaignRole() == CampaignRole.PM
                                && p.getOrganization() != null
                                && p.getOrganization().getType() == OrganizationType.HQ);
                if (hqPm && ids.add(c.getIdx())) visible.add(c);
            });
        }
        return visible;
    }

    /**
     * scope = "mine" → 내가 멤버인 캠페인 (CampaignMember 기준)
     * scope = "org"  → 내 조직이 참여하는 캠페인 (CampaignParticipant 기준)
     * Campaign.tags / partners 는 @BatchSize(50) 으로 배치 로딩되어 N+1 회피.
     */
    public List<CampaignDto.Res> listCampaigns(Long userIdx, String scope) {
        User user = userRepository.findById(userIdx).orElse(null);
        if ("org".equalsIgnoreCase(scope) && user != null && user.getOrganization() != null) {
            Long orgIdx = user.getOrganization().getIdx();
            return participantRepository.findCampaignsByOrganizationIdx(orgIdx).stream()
                    .map(c -> buildResponseFor(c, user))
                    .toList();
        }
        // 기본 — mine
        return memberRepository.findAllWithCampaignByUserIdx(userIdx).stream()
                .map(cm -> buildResponseFor(cm.getCampaign(), user))
                .toList();
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
                .mainMessage(normalizeText(dto.mainMessage()))
                .assetName(normalizeText(dto.assetName()))
                .assetDescription(normalizeText(dto.assetDescription()))
                .primaryGoal(normalizeText(dto.primaryGoal()))
                .campaignMethods(normalizeList(dto.campaignMethods()))
                .maxCost(normalizeText(dto.maxCost()))
                .minRevenue(normalizeText(dto.minRevenue()))
                .ownerName(normalizeText(dto.ownerName()))
                .ownerEmail(normalizeText(dto.ownerEmail()))
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

        // KPI cascade: 캠페인 생성 시 상위 OrganizationKpi에 contribution 등록 (옵션)
        if (dto.contributions() != null && !dto.contributions().isEmpty()) {
            contributionService.bulkCreate(saved, dto.contributions());
        }

        // 썸네일 자동 생성 (Phase 4) — 비동기, 응답 안 막음. 키 없으면 조용히 skip.
        thumbnailGenerator.generateAsyncIfMissing(saved.getIdx());

        return buildResponseFor(saved, owner);
    }

    /** 명시적 AI 생성 — 사용자가 직접 트리거 (Phase 4). */
    @Transactional
    public void regenerateThumbnail(String ownerLoginId, Long campaignId) {
        Campaign campaign = getEditableCampaign(ownerLoginId, campaignId);
        // 기존 썸네일 키 비워서 generator가 생성하도록
        String previous = campaign.getThumbnailObjectKey();
        campaign.updateThumbnailObjectKey(null);
        if (previous != null) {
            try { thumbnailStorage.deleteObject(previous); } catch (Exception ignored) { }
        }
        thumbnailGenerator.generateAsyncIfMissing(campaign.getIdx());
    }

    @Transactional
    public CampaignDto.Res updateCampaign(String ownerLoginId, Long campaignId, CampaignDto.UpsertReq dto) {
        Campaign campaign = getEditableCampaign(ownerLoginId, campaignId);
        User user = userRepository.findUserById(ownerLoginId).orElse(null);
        String name = normalizeRequired(dto.name(), "Campaign name is required.");

        campaign.updateDetails(
                name,
                normalizeText(dto.purpose()),
                normalizeList(dto.tags()),
                dto.startDate(),
                dto.endDate(),
                normalizeList(dto.partners()),
                normalizeText(dto.goals()),
                normalizeText(dto.mainMessage()),
                normalizeText(dto.assetName()),
                normalizeText(dto.assetDescription()),
                normalizeText(dto.primaryGoal()),
                normalizeList(dto.campaignMethods()),
                normalizeText(dto.maxCost()),
                normalizeText(dto.minRevenue()),
                normalizeText(dto.ownerName()),
                normalizeText(dto.ownerEmail()),
                createInitials(name),
                normalizeIcon(dto.icon()),
                normalizeColor(dto.color())
        );

        return buildResponseFor(campaign, user);
    }

    @Transactional
    public CampaignDto.Res invitePartners(String ownerLoginId, Long campaignId, CampaignDto.PartnerInviteReq dto) {
        Campaign campaign = getEditableCampaign(ownerLoginId, campaignId);
        User user = userRepository.findUserById(ownerLoginId).orElse(null);
        campaign.updatePartners(normalizeList(dto.partners()));
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
        return buildResponseFor(campaign, user);
    }

    /**
     * 응답 DTO에 호출 유저 기준 권한 정보(내 캠페인 역할, 내 조직이 PM인지)를 채워서 반환한다.
     * user가 null이면 권한 정보는 null/false로 채움.
     */
    private CampaignDto.Res buildResponseFor(Campaign campaign, User user) {
        Long orgIdx = (user != null && user.getOrganization() != null) ? user.getOrganization().getIdx() : null;
        boolean isPmOrg = orgIdx != null && participantRepository.existsByCampaignIdxAndOrganizationIdxAndCampaignRole(
                campaign.getIdx(), orgIdx, CampaignRole.PM);
        CampaignMemberRole myRole = (user == null) ? null : memberRepository
                .findByCampaignIdxAndUserIdx(campaign.getIdx(), user.getIdx())
                .map(CampaignMember::getCampaignRole)
                .orElse(null);
        return CampaignDto.Res.from(campaign, myRole, isPmOrg);
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
