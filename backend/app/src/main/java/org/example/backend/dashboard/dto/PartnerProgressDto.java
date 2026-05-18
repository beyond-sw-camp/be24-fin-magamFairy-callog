package org.example.backend.dashboard.dto;

import java.util.List;

/**
 * recent7d: 실제 KpiDailySnapshot 7개 이상 누적된 경우만 채워짐. 부족하면 빈 배열.
 * delta: 평균 KPI 달성률의 전 분기 대비 변화 (단위 %p). 비교 데이터 없으면 null.
 */
public record PartnerProgressDto(
        Long organizationId,
        String organizationName,
        long totalCampaigns,
        long activeCampaigns,
        Integer averageKpiAchievementPercent,
        Integer delta,                   // 전 분기 평균 대비 %p 변화 (없으면 null)
        List<Integer> recent7d           // 실제 daily snapshot 만. 7일치 못 채우면 빈 배열.
) {}
