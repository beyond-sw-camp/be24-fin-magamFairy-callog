package org.example.backend.campaign.repository;

import org.example.backend.campaign.model.CampaignContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignContentRepository extends JpaRepository<CampaignContent, Long> {
    List<CampaignContent> findByCampaignIdxOrderBySequenceAsc(Long campaignIdx);
}
