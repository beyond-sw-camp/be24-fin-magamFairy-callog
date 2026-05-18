package org.example.backend.kpi.repository;

import org.example.backend.kpi.model.CampaignKpiContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CampaignKpiContributionRepository extends JpaRepository<CampaignKpiContribution, Long> {

    List<CampaignKpiContribution> findAllByCampaign_IdxOrderByIdxAsc(Long campaignIdx);

    List<CampaignKpiContribution> findAllByTargetOrgKpi_IdxOrderByIdxAsc(Long targetOrgKpiIdx);

    Optional<CampaignKpiContribution> findByCampaign_IdxAndTargetOrgKpi_Idx(Long campaignIdx, Long targetOrgKpiIdx);

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

    /**
     * 여러 OrgKpi 의 committed / actual 합계를 단일 GROUP BY 쿼리로.
     * 결과: [orgKpiIdx, sumCommitted, sumActual] (해당 OrgKpi 에 contribution 이 없으면 결과에 미포함).
     * Dashboard quarterGoals 의 N+1 회피용 (이전엔 OrgKpi N개에 대해 2N 쿼리).
     */
    @Query("SELECT c.targetOrgKpi.idx, " +
           "       COALESCE(SUM(c.committedValue), 0), " +
           "       COALESCE(SUM(c.actualValue), 0) " +
           "FROM CampaignKpiContribution c " +
           "WHERE c.targetOrgKpi.idx IN :orgKpiIds " +
           "GROUP BY c.targetOrgKpi.idx")
    List<Object[]> sumByOrgKpiIdxIn(@Param("orgKpiIds") Collection<Long> orgKpiIds);
}
