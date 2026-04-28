package org.example.backend.campaign.repository;

import org.example.backend.campaign.model.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findAllByOwnerLoginIdOrderByIdxDesc(String ownerLoginId);

    Optional<Campaign> findByIdxAndOwnerLoginId(Long idx, String ownerLoginId);
}
