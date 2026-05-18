package org.example.backend.kpi.dto;

import org.example.backend.campaign.model.KpiCategory;
import org.example.backend.kpi.model.EsgCategory;
import org.example.backend.kpi.model.GoalKind;
import org.example.backend.kpi.model.KpiTemplate;
import org.example.backend.kpi.model.TemplateScope;

import java.time.LocalDateTime;

public record KpiTemplateDto(
        Long idx,
        String name,
        String defaultUnit,
        KpiCategory defaultCategory,
        EsgCategory defaultEsgCategory,
        GoalKind defaultKind,
        TemplateScope scope,
        Long ownerOrgId,
        String ownerOrgName,
        Long createdBy,
        LocalDateTime createdAt,
        Integer usageCount
) {
    public static KpiTemplateDto from(KpiTemplate t) {
        return new KpiTemplateDto(
                t.getIdx(),
                t.getName(),
                t.getDefaultUnit(),
                t.getDefaultCategory(),
                t.getDefaultEsgCategory(),
                t.getDefaultKind(),
                t.getScope(),
                t.getOwnerOrg() != null ? t.getOwnerOrg().getIdx() : null,
                t.getOwnerOrg() != null ? t.getOwnerOrg().getName() : null,
                t.getCreatedBy(),
                t.getCreatedAt(),
                t.getUsageCount()
        );
    }
}
