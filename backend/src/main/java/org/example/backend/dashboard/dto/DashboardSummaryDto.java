package org.example.backend.dashboard.dto;

public record DashboardSummaryDto(
        long activeCampaigns,
        Integer averageKpiAchievementPercent,
        long pendingReviews,
        long partnerCount,
        String scope
) {}
