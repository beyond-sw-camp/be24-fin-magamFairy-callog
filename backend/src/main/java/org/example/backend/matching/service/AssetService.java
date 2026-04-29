package org.example.backend.matching.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.matching.model.MarketingAsset;
import org.example.backend.matching.model.MatchingDto;
import org.example.backend.matching.repository.AssetRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssetService {
    private final AssetRepository assetRepository;

    public MatchingDto.AssetRes getAsset(Long idx) {
        return MatchingDto.AssetRes.toDto(assetRepository.getReferenceById(idx));
    }

    public MatchingDto.AssetList getAssetList(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);

        Page<MarketingAsset> result = assetRepository.findAll(pageRequest);

        return MatchingDto.AssetList.toDto(result);
    }
}
