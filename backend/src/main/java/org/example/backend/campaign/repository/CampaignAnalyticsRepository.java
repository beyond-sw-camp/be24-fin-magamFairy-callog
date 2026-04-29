package org.example.backend.campaign.repository;

import org.example.backend.campaign.model.CampaignAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CampaignAnalyticsRepository extends JpaRepository<CampaignAnalytics, Long> {
    Optional<CampaignAnalytics> findByCampaignIdx(Long campaignIdx);
}
