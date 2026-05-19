package org.example.backend.matching.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.redis.DashboardCacheEvictor;
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
    private final DashboardCacheEvictor dashboardCacheEvictor;

    public MatchingDto.AssetRes getAsset(Long idx) {
        return MatchingDto.AssetRes.toDto(assetRepository.findById(idx).orElseThrow(EntityNotFoundException::new));
    }

    public MatchingDto.AssetList getAssetList(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);

        Page<MarketingAsset> result = assetRepository.findAll(pageRequest);

        return MatchingDto.AssetList.toDto(result);
    }

    @Transactional
    public void addAsset(MatchingDto.AddAsset dto, AuthUserDetails user) {
        if (user == null) {
            throw new IllegalArgumentException("로그인 사용자 정보가 없습니다.");
        }

        User userEntity = userRepository.getReferenceById(user.getIdx());
        Organization affiliate = userEntity.getOrganization();
        if (affiliate == null) {
            throw new IllegalArgumentException("사용자 소속 조직 정보가 없습니다.");
        }

        if (EXTERNAL_PARTNER.equals(affiliate.getType())) {
            throw new IllegalArgumentException("외부 파트너는 자산을 등록할 수 없습니다.");
        }

        assetRepository.save(dto.toEntity(affiliate));
        // Dashboard 캐시 무효화 (summary.liveAssetCount, assetCategories 영향)
        dashboardCacheEvictor.evictAll();
    }

    @Transactional
    public void updateAsset(Long idx, MatchingDto.AddAsset dto) {
        MarketingAsset asset = assetRepository.findById(idx).orElseThrow(EntityNotFoundException::new);
        asset.update(dto.getTarget(), dto.getType(), dto.getScale(), dto.getConditions());
        // Dashboard 캐시 무효화 (category 변경 시 assetCategories 영향)
        dashboardCacheEvictor.evictAll();
    }

    @Transactional
    public void deleteAsset(Long idx) {
        if (!assetRepository.existsById(idx)) {
            throw new EntityNotFoundException();
        }
        assetRepository.deleteById(idx);
        // Dashboard 캐시 무효화 (summary.liveAssetCount, assetCategories 영향)
        dashboardCacheEvictor.evictAll();
    }
}
