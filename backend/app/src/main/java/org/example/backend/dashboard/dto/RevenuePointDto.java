package org.example.backend.dashboard.dto;

import java.math.BigDecimal;

/** 매출 추이 단일 포인트 (라벨 + 값). 분기/월 등 범용. */
public record RevenuePointDto(
        String label,
        BigDecimal value
) {}
