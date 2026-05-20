package org.example.backend.dashboard.dto;

import java.math.BigDecimal;

/** Zone4 P2 매출 추이 라인 — 월 라벨 + 값. */
public record LabelValueDto(
        String label,
        BigDecimal value
) {}
