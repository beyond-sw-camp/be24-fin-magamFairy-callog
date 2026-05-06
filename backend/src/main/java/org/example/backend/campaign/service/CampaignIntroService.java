package org.example.backend.campaign.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignIntro;
import org.example.backend.campaign.model.CampaignIntroDto;
import org.example.backend.campaign.repository.CampaignIntroRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CampaignIntroService {

    private final CampaignIntroRepository introRepository;
    private final CampaignRepository campaignRepository;

    @Transactional(readOnly = true)
    public CampaignIntroDto.GetRes getIntro(Long campaignIdx) {
        Campaign campaign = campaignRepository.findById(campaignIdx)
                .orElseThrow(() -> new EntityNotFoundException("캠페인을 찾을 수 없습니다. id=" + campaignIdx));

        CampaignIntro intro = introRepository.findByCampaign_Idx(campaignIdx).orElse(null);

        return CampaignIntroDto.GetRes.toDto(intro, campaign);
    }

    @Transactional
    public void updateIntro(Long campaignIdx, CampaignIntroDto.UpdateReq dto) {
        Campaign campaign = campaignRepository.findById(campaignIdx)
                .orElseThrow(() -> new EntityNotFoundException("캠페인을 찾을 수 없습니다. id=" + campaignIdx));

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
    }
}
