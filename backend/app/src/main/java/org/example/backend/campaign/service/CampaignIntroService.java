package org.example.backend.campaign.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignIntro;
import org.example.backend.campaign.model.CampaignIntroDto;
import org.example.backend.campaign.model.CampaignMember;
import org.example.backend.campaign.model.CampaignMemberRole;
import org.example.backend.campaign.model.CampaignRole;
import org.example.backend.campaign.repository.CampaignIntroRepository;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.campaign.repository.CampaignParticipantRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.notification.service.NotificationSseService;
import org.example.backend.organization.model.OrganizationType;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CampaignIntroService {

    private final CampaignIntroRepository introRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final CampaignParticipantRepository participantRepository;
    private final CampaignMemberRepository memberRepository;
    private final NotificationSseService sseService;

    @Transactional
    public CampaignIntroDto.GetRes getIntro(Long campaignIdx, Long callerIdx) {
        Campaign campaign = campaignRepository.findById(campaignIdx)
                .orElseThrow(() -> new EntityNotFoundException("캠페인을 찾을 수 없습니다. id=" + campaignIdx));

        CampaignIntro intro = introRepository.findByCampaign_Idx(campaignIdx).orElse(null);

        boolean canEdit = canCallerEdit(callerIdx, campaignIdx);
        boolean internalViewer = isCallerInternal(callerIdx);

        // 조회 수 증가 — 편집 권한 없는 외부 뷰어만 카운트 (PM 팀 자기 페이지 보는 건 제외)
        if (intro != null && !canEdit) {
            introRepository.incrementViewCount(intro.getIdx());
            intro.recordView();
        }

        // 담당자 = 캠페인 생성자(ownerLoginId의 user 계정)
        User ownerUser = campaign.getOwnerLoginId() == null ? null
                : userRepository.findUserById(campaign.getOwnerLoginId()).orElse(null);
        return CampaignIntroDto.GetRes.toDto(intro, campaign, canEdit, internalViewer, ownerUser);
    }

    @Transactional
    public void updateIntro(Long campaignIdx, CampaignIntroDto.UpdateReq dto, Long callerIdx) {
        Campaign campaign = campaignRepository.findById(campaignIdx)
                .orElseThrow(() -> new EntityNotFoundException("캠페인을 찾을 수 없습니다. id=" + campaignIdx));

        // P0 — 편집 권한 검증: PM 조직이고 캠페인 멤버 GM/MGR 인 경우만
        if (!canCallerEdit(callerIdx, campaignIdx)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "캠페인 소개 페이지를 편집할 권한이 없습니다.");
        }

        // visibility 변경 — Campaign entity 자체 필드 갱신
        if (dto.getVisibility() != null) {
            campaign.updateVisibility(dto.getVisibility());
        }

        Optional<CampaignIntro> existing = introRepository.findByCampaign_Idx(campaignIdx);
        if (existing.isPresent()) {
            existing.get().updateContent(dto);
        } else {
            CampaignIntro intro = CampaignIntro.builder()
                    .campaign(campaign)
                    .rfpCode(dto.getRfpCode())
                    .recruitDeadline(dto.getRecruitDeadline())
                    .hanwhaAssets(dto.getHanwhaAssets())
                    .partnerRoles(dto.getPartnerRoles())
                    .customerTags(dto.getCustomerTags())
                    .partnerValues(dto.getPartnerValues())
                    .timelineEvents(dto.getTimelineEvents())
                    .submissionDocs(dto.getSubmissionDocs())
                    .attachedFiles(dto.getAttachedFiles())
                    .contactInfo(dto.getContactInfo())
                    .weightCustomer(dto.getWeightCustomer())
                    .weightRevenue(dto.getWeightRevenue())
                    .weightCost(dto.getWeightCost())
                    .weightOperation(dto.getWeightOperation())
                    .weightBrand(dto.getWeightBrand())
                    .build();
            introRepository.save(intro);
        }

        sseService.broadcastCalendarRefresh(campaignIdx, "deadline");
    }

    /** PM 조직이고 캠페인 멤버 GM/MGR 인 경우만 편집 가능. */
    private boolean canCallerEdit(Long callerIdx, Long campaignIdx) {
        if (callerIdx == null) return false;
        User user = userRepository.findById(callerIdx).orElse(null);
        if (user == null || user.getOrganization() == null) return false;
        boolean isPmOrg = participantRepository.existsByCampaignIdxAndOrganizationIdxAndCampaignRole(
                campaignIdx, user.getOrganization().getIdx(), CampaignRole.PM);
        if (!isPmOrg) return false;
        CampaignMemberRole myRole = memberRepository
                .findByCampaignIdxAndUserIdx(campaignIdx, user.getIdx())
                .map(CampaignMember::getCampaignRole)
                .orElse(null);
        return myRole == CampaignMemberRole.MANAGER || myRole == CampaignMemberRole.GENERAL_MANAGER;
    }

    /** 내부 사용자(HQ/AFFILIATE) — 매칭 가중치·심사 기준 등 내부 정보 조회 가능. EXTERNAL_PARTNER 는 false. */
    private boolean isCallerInternal(Long callerIdx) {
        if (callerIdx == null) return false;
        User user = userRepository.findById(callerIdx).orElse(null);
        if (user == null || user.getOrganization() == null) return false;
        OrganizationType type = user.getOrganization().getType();
        return type == OrganizationType.HQ || type == OrganizationType.AFFILIATE;
    }
}
