package org.example.backend.dashboard.dto;

import java.math.BigDecimal;

/**
 * Zone4 P2 매출 YoY 듀얼 막대의 한 달 포인트.
 * value = 선택 연도 해당 월 실적, prev = 전년 동월 실적.
 */
public record RevenueYoYPointDto(
        String label,       // 예: "5월"
        BigDecimal value,   // 올해 실적
        BigDecimal prev     // 작년 동월 실적
) {}
