package org.example.backend.dashboard.dto;

import java.util.List;

/**
 * recent7d: 최근 7일 sparkline 데이터.
 * KpiDailySnapshot 테이블이 없으므로 평균 점수 기반 stub.
 */
public record PartnerProgressDto(
        Long organizationId,
        String organizationName,
        long totalCampaigns,
        long activeCampaigns,
        Integer averageKpiAchievementPercent,
        List<Integer> recent7d
) {}
