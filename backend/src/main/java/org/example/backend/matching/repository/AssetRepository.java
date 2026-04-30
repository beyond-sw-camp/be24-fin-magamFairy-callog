package org.example.backend.matching.repository;

import org.example.backend.matching.model.MarketingAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository extends JpaRepository<MarketingAsset, Long> {
}
