package org.example.backend.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * monthlyActuals / monthlyTargets: 분기 내 3개월 분배.
 * KpiMonthlySnapshot 이 있는 달은 정확한 값, 현재 진행 월은 누적값으로 표시, 과거 미수집 달은 null.
 */
public record QuarterGoalProgressDto(
        Long orgKpiId,
        String name,
        String unit,
        BigDecimal targetValue,
        BigDecimal committedSum,
        BigDecimal actualValue,           // 분기 누적 실적값 (frontend Z1/P2 KPI 카드의 액수 표시용)
        Integer achievementPercent,
        String periodCode,
        Long ownerOrgId,
        String ownerOrgName,
        String category,         // KpiCategory.name() 또는 null
        String esgCategory,      // EsgCategory.name() 또는 null
        List<Integer> monthlyActuals,    // null 가능 — snapshot 없는 달은 null
        List<Integer> monthlyTargets
) {}
