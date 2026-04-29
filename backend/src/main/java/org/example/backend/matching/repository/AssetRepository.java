package org.example.backend.matching.repository;

import org.example.backend.matching.model.MarketingAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<MarketingAsset, Long> {
}
