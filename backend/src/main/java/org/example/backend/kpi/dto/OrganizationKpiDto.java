package org.example.backend.kpi.dto;

import org.example.backend.campaign.model.KpiCategory;
import org.example.backend.kpi.model.EsgCategory;
import org.example.backend.kpi.model.GoalKind;
import org.example.backend.kpi.model.GoalPeriodType;
import org.example.backend.kpi.model.GoalStatus;
import org.example.backend.kpi.model.OrganizationKpi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record OrganizationKpiDto(
        Long idx,
        Long ownerOrgId,
        String ownerOrgName,
        String ownerOrgType,
        Long parentKpiId,
        String parentKpiName,
        BigDecimal contributionToParent,
        String name,
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
        GoalStatus status,
        String achievabilityNote,
        Long templateId,
        Long previousVersionId,
        Long createdBy,
        LocalDateTime createdAt,
        Long updatedBy,
        LocalDateTime updatedAt,
        Long approvedBy,
        LocalDateTime approvedAt
) {
    public static OrganizationKpiDto from(OrganizationKpi k) {
        return new OrganizationKpiDto(
                k.getIdx(),
                k.getOwner() != null ? k.getOwner().getIdx() : null,
                k.getOwner() != null ? k.getOwner().getName() : null,
                k.getOwner() != null && k.getOwner().getType() != null
                        ? k.getOwner().getType().name() : null,
                k.getParentKpi() != null ? k.getParentKpi().getIdx() : null,
                k.getParentKpi() != null ? k.getParentKpi().getName() : null,
                k.getContributionToParent(),
                k.getName(),
                k.getPeriodType(),
                k.getPeriodCode(),
                k.getPeriodStart(),
                k.getPeriodEnd(),
                k.getTargetValue(),
                k.getActualValue(),
                k.getUnit(),
                k.getCategory(),
                k.getEsgCategory(),
                k.getKind(),
                k.getStatus(),
                k.getAchievabilityNote(),
                k.getTemplateId(),
                k.getPreviousVersionId(),
                k.getCreatedBy(),
                k.getCreatedAt(),
                k.getUpdatedBy(),
                k.getUpdatedAt(),
                k.getApprovedBy(),
                k.getApprovedAt()
        );
    }
}
