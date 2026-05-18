package org.example.backend.kpi.repository;

import org.example.backend.kpi.model.KpiTemplate;
import org.example.backend.kpi.model.TemplateScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KpiTemplateRepository extends JpaRepository<KpiTemplate, Long> {

    List<KpiTemplate> findAllByOrderByIdxDesc();

    List<KpiTemplate> findAllByScopeOrderByIdxDesc(TemplateScope scope);

    @Query("SELECT t FROM KpiTemplate t " +
           "WHERE (:scope IS NULL OR t.scope = :scope) " +
           "AND (:orgIdx IS NULL OR t.ownerOrg.idx = :orgIdx OR t.scope = org.example.backend.kpi.model.TemplateScope.GLOBAL) " +
           "ORDER BY t.idx DESC")
    List<KpiTemplate> findByFilters(
            @Param("scope") TemplateScope scope,
            @Param("orgIdx") Long orgIdx);
}
