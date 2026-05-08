package org.example.backend.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * monthlyActuals / monthlyTargets: 분기 내 3개월 분배.
 * KpiMonthlySnapshot 테이블이 없으므로 actualSum/targetValue를 균등 분배 stub.
 * Phase 2에서 월별 스냅샷 테이블 도입 시 정확한 분배로 교체.
 */
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
        String ownerOrgName,
        String category,         // KpiCategory.name() 또는 null
        String esgCategory,      // EsgCategory.name() 또는 null
        List<Integer> monthlyActuals,    // null 가능 — snapshot 없는 달은 null
        List<Integer> monthlyTargets
) {}
