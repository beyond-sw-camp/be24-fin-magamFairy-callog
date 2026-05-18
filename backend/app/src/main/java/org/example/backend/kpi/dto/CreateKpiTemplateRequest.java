package org.example.backend.kpi.dto;

import org.example.backend.campaign.model.KpiCategory;
import org.example.backend.kpi.model.EsgCategory;
import org.example.backend.kpi.model.GoalKind;
import org.example.backend.kpi.model.TemplateScope;

public record CreateKpiTemplateRequest(
        String name,
        String defaultUnit,
        KpiCategory defaultCategory,
        EsgCategory defaultEsgCategory,
        GoalKind defaultKind,
        TemplateScope scope,
        Long ownerOrgId
) {}
