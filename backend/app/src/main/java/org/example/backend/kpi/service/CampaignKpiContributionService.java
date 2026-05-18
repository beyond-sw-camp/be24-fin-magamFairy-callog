package org.example.backend.kpi.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignMember;
import org.example.backend.campaign.model.CampaignMemberRole;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.common.security.CampaignMemberGuard;
import org.example.backend.kpi.dto.CampaignKpiContributionDto;
import org.example.backend.kpi.dto.CreateContributionRequest;
import org.example.backend.kpi.model.CampaignKpiContribution;
import org.example.backend.kpi.model.OrganizationKpi;
import org.example.backend.kpi.repository.CampaignKpiContributionRepository;
import org.example.backend.kpi.repository.OrganizationKpiRepository;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignKpiContributionService {

    private final CampaignKpiContributionRepository contributionRepository;
    private final OrganizationKpiRepository orgKpiRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository memberRepository;
    private final UserRepository userRepository;

    public List<CampaignKpiContributionDto> list(Long campaignId) {
        return contributionRepository.findAllByCampaign_IdxOrderByIdxAsc(campaignId).stream()
                .map(CampaignKpiContributionDto::from)
                .toList();
    }

    @Transactional
    public CampaignKpiContributionDto create(Long callerIdx, Long campaignId, CreateContributionRequest req) {
        User caller = findUser(callerIdx);
        Campaign campaign = findCampaign(campaignId);
        requireEditable(campaign, caller);

        if (req.targetOrgKpiId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetOrgKpiId는 필수입니다.");
        }
        if (req.committedValue() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "committedValue는 필수입니다.");
        }

        OrganizationKpi target = orgKpiRepository.findById(req.targetOrgKpiId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OrganizationKpi not found."));

        CampaignKpiContribution contribution = CampaignKpiContribution.builder()
                .campaign(campaign)
                .targetOrgKpi(target)
                .committedValue(req.committedValue())
                .actualValue(req.actualValue() == null ? BigDecimal.ZERO : req.actualValue())
                .build();

        return CampaignKpiContributionDto.from(contributionRepository.save(contribution));
    }

    @Transactional
    public CampaignKpiContributionDto update(Long callerIdx, Long campaignId, Long contributionId,
                                             BigDecimal committedValue, BigDecimal actualValue) {
        User caller = findUser(callerIdx);
        Campaign campaign = findCampaign(campaignId);
        requireEditable(campaign, caller);

        CampaignKpiContribution contribution = findContribution(campaignId, contributionId);
        if (committedValue != null) contribution.setCommittedValue(committedValue);
        if (actualValue != null) contribution.setActualValue(actualValue);

        return CampaignKpiContributionDto.from(contribution);
    }

    @Transactional
    public void delete(Long callerIdx, Long campaignId, Long contributionId) {
        User caller = findUser(callerIdx);
        Campaign campaign = findCampaign(campaignId);
        requireEditable(campaign, caller);

        CampaignKpiContribution contribution = findContribution(campaignId, contributionId);
        contributionRepository.delete(contribution);
    }

    /**
     * 캠페인 생성 시 일괄 등록 헬퍼 (CampaignService에서 호출).
     */
    @Transactional
    public void bulkCreate(Campaign campaign, List<CreateContributionRequest> requests) {
        if (requests == null || requests.isEmpty()) return;
        for (CreateContributionRequest req : requests) {
            if (req == null || req.targetOrgKpiId() == null || req.committedValue() == null) {
                continue;
            }
            OrganizationKpi target = orgKpiRepository.findById(req.targetOrgKpiId()).orElse(null);
            if (target == null) continue;
            CampaignKpiContribution contribution = CampaignKpiContribution.builder()
                    .campaign(campaign)
                    .targetOrgKpi(target)
                    .committedValue(req.committedValue())
                    .actualValue(req.actualValue() == null ? BigDecimal.ZERO : req.actualValue())
                    .build();
            contributionRepository.save(contribution);
        }
    }

    // ── Helper ──────────────────────────────────────────────

    private User findUser(Long userIdx) {
        return userRepository.findById(userIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }

    private Campaign findCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found."));
    }

    private CampaignKpiContribution findContribution(Long campaignId, Long contributionId) {
        CampaignKpiContribution c = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contribution not found."));
        if (!Objects.equals(c.getCampaign().getIdx(), campaignId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "캠페인에 속하지 않는 contribution입니다.");
        }
        return c;
    }

    /**
     * 캠페인 멤버이면서 MANAGER/GENERAL_MANAGER 만 편집 가능.
     */
    private void requireEditable(Campaign campaign, User caller) {
        if (campaign.isClosed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "종료된 캠페인은 수정할 수 없습니다.");
        }
        CampaignMember me = memberRepository
                .findByCampaignIdxAndUserIdx(campaign.getIdx(), caller.getIdx())
                .orElse(null);
        CampaignMemberGuard.requireMember(me);
        if (me.getCampaignRole() == CampaignMemberRole.USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "편집 권한이 없습니다.");
        }
    }
}
