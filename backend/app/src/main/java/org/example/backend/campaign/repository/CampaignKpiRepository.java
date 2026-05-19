package org.example.backend.campaign.repository;

import org.example.backend.campaign.model.CampaignKpi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CampaignKpiRepository extends JpaRepository<CampaignKpi, Long> {
    List<CampaignKpi> findAllByCampaignIdxOrderByIdxAsc(Long campaignIdx);
    boolean existsByCampaignIdx(Long campaignIdx);

    /**
     * 여러 캠페인의 KPI 를 단일 IN 쿼리로 (N+1 회피).
     * Dashboard summary / partnerProgress / kpiCategories 에서 사용.
     */
    List<CampaignKpi> findAllByCampaign_IdxInOrderByIdxAsc(Collection<Long> campaignIds);
}
