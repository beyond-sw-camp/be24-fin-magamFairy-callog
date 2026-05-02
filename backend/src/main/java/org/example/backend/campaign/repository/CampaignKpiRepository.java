package org.example.backend.campaign.repository;

import org.example.backend.campaign.model.CampaignKpi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignKpiRepository extends JpaRepository<CampaignKpi, Long> {
    List<CampaignKpi> findAllByCampaignIdxOrderByIdxAsc(Long campaignIdx);
    boolean existsByCampaignIdx(Long campaignIdx);
}
