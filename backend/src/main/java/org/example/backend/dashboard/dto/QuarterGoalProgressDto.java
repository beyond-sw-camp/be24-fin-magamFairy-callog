package org.example.backend.dashboard.dto;

import java.math.BigDecimal;

public record QuarterGoalProgressDto(
        Long orgKpiId,
        String name,
        String unit,
        BigDecimal targetValue,
        BigDecimal committedSum,
        BigDecimal actualSum,
        Integer achievementPercent,
        String periodCode,
        Long ownerOrgId,
        String ownerOrgName
) {}
