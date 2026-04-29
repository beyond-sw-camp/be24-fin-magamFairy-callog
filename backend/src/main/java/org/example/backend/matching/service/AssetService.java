package org.example.backend.matching.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.matching.model.MarketingAsset;
import org.example.backend.matching.model.MatchingDto;
import org.example.backend.matching.repository.AssetRepository;
import org.example.backend.organization.model.Organization;
import org.example.backend.user.model.AuthUserDetails;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import static org.example.backend.organization.model.OrganizationType.EXTERNAL_PARTNER;

@Service
@RequiredArgsConstructor
public class AssetService {
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;

    public MatchingDto.AssetRes getAsset(Long idx) {
        return MatchingDto.AssetRes.toDto(assetRepository.getReferenceById(idx));
    }

    public MatchingDto.AssetList getAssetList(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);

        Page<MarketingAsset> result = assetRepository.findAll(pageRequest);

        return MatchingDto.AssetList.toDto(result);
    }

    @Transactional
    public void addAsset(MatchingDto.AddAsset dto, AuthUserDetails user) {
        User userEntity = userRepository.findById(user.getIdx()).orElseThrow();
        Organization affiliate = userEntity.getOrganization();
        if(affiliate.getType().equals(EXTERNAL_PARTNER)){
            System.out.println("뭔가 안됨");
        }else {
            assetRepository.save(dto.toEntity(affiliate));
        }
    }
}
