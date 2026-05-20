package org.example.backend.kpi.dto;

import org.example.backend.campaign.model.KpiCategory;
import org.example.backend.kpi.model.EsgCategory;
import org.example.backend.kpi.model.GoalKind;
import org.example.backend.kpi.model.GoalPeriodType;
import org.example.backend.kpi.model.GoalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateOrganizationKpiRequest(
        String name,
        Long ownerOrgId,
        Long parentKpiId,
        BigDecimal contributionToParent,
        GoalPeriodType periodType,
        String periodCode,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal targetValue,
        String unit,
        KpiCategory category,
        EsgCategory esgCategory,
        GoalKind kind,
        GoalStatus status,
        String achievabilityNote,
        Long templateId,
        Boolean visibleToAffiliate   // HQ KPI 를 계열사에 노출할지 (HQ 전용 토글)
) {}
