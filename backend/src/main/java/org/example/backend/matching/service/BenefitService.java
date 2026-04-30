package org.example.backend.matching.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

import java.util.NoSuchElementException;

import static org.example.backend.organization.model.OrganizationType.EXTERNAL_PARTNER;

@Service
@RequiredArgsConstructor
public class BenefitService {
    private final BenefitRepository benefitRepository;
    private final UserRepository userRepository;

    public MatchingDto.BenefitRes getBenefit(Long idx) {
        return MatchingDto.BenefitRes.toDto(benefitRepository.findById(idx).orElseThrow(NoSuchElementException::new));
    }

    public MatchingDto.BenefitList getBenefitList(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);

        Page<PartnerBenefits> result = benefitRepository.findAll(pageRequest);

        return MatchingDto.BenefitList.toDto(result);
    }

    @Transactional
    public void addBenefit(MatchingDto.AddBenefit dto, AuthUserDetails user) {
        User userEntity = userRepository.getReferenceById(user.getIdx());
        Organization affiliate = userEntity.getOrganization();
        if(affiliate.getType().equals(EXTERNAL_PARTNER)){
            System.out.println("권한 거부");
        }else {
            benefitRepository.save(dto.toEntity(affiliate));
        }
    }
}
