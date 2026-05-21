package org.example.backend.kpi.dto;

import org.example.backend.campaign.model.KpiCategory;
import org.example.backend.kpi.model.EsgCategory;
import org.example.backend.kpi.model.GoalKind;
import org.example.backend.kpi.model.GoalPeriodType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateOrganizationKpiRequest(
        String name,
        Long parentKpiId,
        BigDecimal contributionToParent,
        GoalPeriodType periodType,
        String periodCode,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal targetValue,
        BigDecimal actualValue,
        String unit,
        KpiCategory category,
        EsgCategory esgCategory,
        GoalKind kind,
        String achievabilityNote,
        Boolean visibleToAffiliate   // HQ KPI 계열사 노출 토글
) {}
