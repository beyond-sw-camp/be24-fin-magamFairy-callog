package org.example.backend.kpi.dto;

import java.math.BigDecimal;

public record CreateContributionRequest(
        Long targetOrgKpiId,
        BigDecimal committedValue,
        BigDecimal actualValue
) {}
