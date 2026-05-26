package org.example.backend.dashboard.dto;

import java.util.List;
import java.util.Map;

/**
 * 프론트엔드 dashboardStore.loadAll() 이 사용하는 대시보드 통합 응답.
 *
 * kpiCategories 는 개별 endpoint 로는 계속 제공하지만,
 * 현재 dashboard store 에서 사용하지 않으므로 통합 응답에는 포함하지 않는다.
 */
public record DashboardPageDto(
        DashboardSummaryDto summary,
        List<QuarterGoalProgressDto> quarterGoals,
        List<PartnerProgressDto> partnerProgress,
        Map<String, Long> assetCategories
) {}
