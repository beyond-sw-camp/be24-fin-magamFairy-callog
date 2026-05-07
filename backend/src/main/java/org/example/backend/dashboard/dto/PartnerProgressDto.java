package org.example.backend.dashboard.dto;

public record PartnerProgressDto(
        Long organizationId,
        String organizationName,
        long totalCampaigns,
        long activeCampaigns,
        Integer averageKpiAchievementPercent
) {}
