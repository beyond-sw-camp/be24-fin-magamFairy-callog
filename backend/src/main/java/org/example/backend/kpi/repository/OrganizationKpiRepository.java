package org.example.backend.kpi.repository;

import org.example.backend.kpi.model.GoalStatus;
import org.example.backend.kpi.model.OrganizationKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrganizationKpiRepository extends JpaRepository<OrganizationKpi, Long> {

    List<OrganizationKpi> findAllByOrderByIdxDesc();

    List<OrganizationKpi> findAllByOwner_IdxOrderByIdxDesc(Long ownerOrgIdx);

    List<OrganizationKpi> findAllByOwner_IdxAndStatusOrderByIdxDesc(Long ownerOrgIdx, GoalStatus status);

    List<OrganizationKpi> findAllByPeriodCodeOrderByIdxDesc(String periodCode);

    List<OrganizationKpi> findAllByStatusOrderByIdxDesc(GoalStatus status);

    @Query("SELECT k FROM OrganizationKpi k " +
           "WHERE (:ownerOrgIdx IS NULL OR k.owner.idx = :ownerOrgIdx) " +
           "AND (:periodCode IS NULL OR k.periodCode = :periodCode) " +
           "AND (:status IS NULL OR k.status = :status) " +
           "ORDER BY k.idx DESC")
    List<OrganizationKpi> findByFilters(
            @Param("ownerOrgIdx") Long ownerOrgIdx,
            @Param("periodCode") String periodCode,
            @Param("status") GoalStatus status);

    @Query("SELECT k FROM OrganizationKpi k " +
           "WHERE (:ownerOrgIdx IS NULL OR k.owner.idx = :ownerOrgIdx) " +
           "AND k.periodCode LIKE CONCAT(:year, '-%') " +
           "AND (:status IS NULL OR k.status = :status) " +
           "ORDER BY k.idx DESC")
    List<OrganizationKpi> findByFiltersForYear(
            @Param("ownerOrgIdx") Long ownerOrgIdx,
            @Param("year") String year,
            @Param("status") GoalStatus status);

    /**
     * cascade parent 후보 - 상위 조직(또는 본인 조직)이 보유한 ACTIVE KPI 목록.
     * orgId가 null이면 전체 ACTIVE.
     */
    @Query("SELECT k FROM OrganizationKpi k " +
           "WHERE k.status = org.example.backend.kpi.model.GoalStatus.ACTIVE " +
           "AND (:orgIdx IS NULL OR k.owner.idx = :orgIdx) " +
           "ORDER BY k.idx DESC")
    List<OrganizationKpi> findActiveParentCandidates(@Param("orgIdx") Long orgIdx);

    /**
     * 자손 KPI 조회 (cascade 루프 검증용).
     * 주어진 KPI를 parent로 가지는 모든 자식 KPI.
     */
    List<OrganizationKpi> findAllByParentKpi_Idx(Long parentKpiIdx);

    boolean existsByParentKpi_Idx(Long parentKpiIdx);
}
