package org.example.backend.campaign.repository;

import org.example.backend.campaign.model.CampaignVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignVersionRepository extends JpaRepository<CampaignVersion, Long> {
    List<CampaignVersion> findByCampaignIdxOrderByVersionNumberDesc(Long campaignIdx);
}
