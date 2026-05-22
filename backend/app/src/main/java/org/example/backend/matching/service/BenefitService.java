package org.example.backend.matching.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.common.redis.DashboardCacheEvictor;
import org.example.backend.matching.model.MarketingAsset;
import org.example.backend.matching.model.MatchingDto;
import org.example.backend.matching.model.PartnerBenefits;
import org.example.backend.matching.repository.AssetRepository;
import org.example.backend.matching.repository.BenefitRepository;
import org.example.backend.organization.model.Organization;
import org.example.backend.user.model.AuthUserDetails;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.example.backend.organization.model.OrganizationType.EXTERNAL_PARTNER;

@Service
@RequiredArgsConstructor
public class BenefitService {
    private final BenefitRepository benefitRepository;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final DashboardCacheEvictor dashboardCacheEvictor;

    public List<MatchingDto.BenefitRes> getBenefit(String publicId) {
        Long campaignIdx = campaignRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("해당 캠페인을 찾을 수 없습니다. publicId: " + publicId))
                .getIdx();

        List<PartnerBenefits> benefits = benefitRepository.findAllByCampaignIdx(campaignIdx);

        if (benefits.isEmpty()) {
            throw new NoSuchElementException("해당 캠페인에 등록된 혜택이 존재하지 않습니다.");
        }
        return benefits.stream()
                .map(MatchingDto.BenefitRes::toDto)
                .collect(Collectors.toList());
    }

    public MatchingDto.BenefitList getBenefitList(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);

        Page<PartnerBenefits> result = benefitRepository.findAll(pageRequest);

        return MatchingDto.BenefitList.toDto(result);
    }

    @Transactional
    public void addBenefit(MatchingDto.AddBenefit dto, Long userIdx) {
        User userEntity = userRepository.getReferenceById(userIdx);
        Organization affiliate = userEntity.getOrganization();
        Campaign campaign = campaignRepository.findById(dto.getCampaignIdx()).orElseThrow();

        if(affiliate.getType().equals(EXTERNAL_PARTNER)){
            System.out.println("권한 거부");
        }else {
            benefitRepository.save(dto.toEntity(affiliate, campaign));
            // Dashboard 캐시 무효화 (summary.rfpCount 영향)
            dashboardCacheEvictor.evictAll();
        }
    }
}
