package org.example.backend.dashboard.dto;

import java.util.List;

/**
 * 본사 대시보드 상단 KPI 6-up 데이터.
 * frontend mock 호환 필드(progressPct, miniStats, trend) 포함.
 *
 * miniStats: [검수 패스율, 매칭 평균, 자산 LIVE] 순서.
 */
public record DashboardSummaryDto(
        long activeCampaigns,
        Integer averageKpiAchievementPercent,
        Integer progressPct,        // = averageKpiAchievementPercent (frontend 호환)
        long pendingReviews,
        long partnerCount,
        long newPartnerCount,        // 30일 내 신규 (현재는 partnerCount stub)
        long rfpCount,               // RFP 응모(=PartnerBenefits) 카운트
        Integer trend,               // 지난주 대비 +%p (Phase 2 stub: 0)
        List<MiniStat> miniStats,    // [검수 패스율, 매칭 평균, 자산 LIVE]
        String scope
) {
    public record MiniStat(String label, String value) {}
}
