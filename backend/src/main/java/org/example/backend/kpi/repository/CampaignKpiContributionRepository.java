package org.example.backend.kpi.repository;

import org.example.backend.kpi.model.CampaignKpiContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CampaignKpiContributionRepository extends JpaRepository<CampaignKpiContribution, Long> {

    List<CampaignKpiContribution> findAllByCampaign_IdxOrderByIdxAsc(Long campaignIdx);

    List<CampaignKpiContribution> findAllByTargetOrgKpi_IdxOrderByIdxAsc(Long targetOrgKpiIdx);

    boolean existsByTargetOrgKpi_Idx(Long targetOrgKpiIdx);

    /**
     * 진행 중 캠페인이 contribution으로 참조하는지 검사 — KPI 정의 잠금 판단용.
     * 진행 중: status in ('live', 'review', 'paused') — closed/draft 제외.
     */
    @Query("SELECT COUNT(c) > 0 FROM CampaignKpiContribution c " +
           "WHERE c.targetOrgKpi.idx = :orgKpiIdx " +
           "AND c.campaign.status IN ('live', 'review', 'paused')")
    boolean existsActiveCampaignReferencing(@Param("orgKpiIdx") Long orgKpiIdx);

    @Query("SELECT COALESCE(SUM(c.actualValue), 0) FROM CampaignKpiContribution c " +
           "WHERE c.targetOrgKpi.idx = :orgKpiIdx")
    BigDecimal sumActualByTargetOrgKpi(@Param("orgKpiIdx") Long orgKpiIdx);

    @Query("SELECT COALESCE(SUM(c.committedValue), 0) FROM CampaignKpiContribution c " +
           "WHERE c.targetOrgKpi.idx = :orgKpiIdx")
    BigDecimal sumCommittedByTargetOrgKpi(@Param("orgKpiIdx") Long orgKpiIdx);
}
