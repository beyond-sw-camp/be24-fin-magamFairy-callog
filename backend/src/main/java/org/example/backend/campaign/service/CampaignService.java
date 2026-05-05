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

    public List<CampaignDto.Res> listCampaigns(Long userIdx) {
        User user = userRepository.findById(userIdx).orElse(null);
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
                .status("draft")
                .initials(createInitials(name))
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

        return buildResponseFor(saved, owner);
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
                createInitials(name),
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
