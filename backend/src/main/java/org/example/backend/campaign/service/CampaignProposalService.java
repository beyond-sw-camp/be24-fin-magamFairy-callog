package org.example.backend.campaign.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignProposalDto;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.matching.model.MarketingAsset;
import org.example.backend.matching.model.PartnerBenefits;
import org.example.backend.matching.repository.AssetRepository;
import org.example.backend.matching.repository.BenefitRepository;
import org.example.backend.organization.model.Organization;
import org.example.backend.user.model.AuthUserDetails;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CampaignProposalService {

    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final BenefitRepository benefitRepository;

    @Transactional
    public CampaignProposalDto.SubmitRes submitProposal(
            Long campaignIdx,
            CampaignProposalDto.SubmitReq dto,
            AuthUserDetails authUser
    ) {
        Campaign campaign = campaignRepository.findById(campaignIdx)
                .orElseThrow(() -> new EntityNotFoundException("캠페인을 찾을 수 없습니다. id=" + campaignIdx));

        User user = userRepository.findById(authUser.getIdx())
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        Organization organization = user.getOrganization();
        if (organization == null) {
            throw new IllegalStateException("소속 조직 정보가 없습니다. 제안서를 제출하려면 조직에 소속되어야 합니다.");
        }

        MarketingAsset asset = MarketingAsset.builder()
                .organization(organization)
                .campaign(campaign)
                .target(dto.getAsset().getTarget())
                .type(dto.getAsset().getType())
                .scale(dto.getAsset().getScale())
                .conditions(dto.getAsset().getConditions())
     //           .isActive(true)
                .build();
        MarketingAsset savedAsset = assetRepository.save(asset);

        PartnerBenefits benefit = PartnerBenefits.builder()
                .organization(organization)
                .campaign(campaign)
                .name(dto.getBenefit().getName())
                .type(dto.getBenefit().getType())
                .scale(dto.getBenefit().getScale())
                .target(dto.getBenefit().getTarget())
                .cost(dto.getBenefit().getCost())
                .status(dto.getBenefit().getStatus() != null ? dto.getBenefit().getStatus() : "PENDING")
                .build();
        PartnerBenefits savedBenefit = benefitRepository.save(benefit);

        return CampaignProposalDto.SubmitRes.builder()
                .assetIdx(savedAsset.getIdx())
                .benefitIdx(savedBenefit.getIdx())
                .build();
    }
}
