package org.example.backend.dashboard.dto;

public record BlockerDto(
        String type,            // "TASK_BLOCKED" 또는 "CAMPAIGN_NO_GM"
        Long targetId,
        String targetName,
        Long campaignId,
        String campaignName,
        String description
) {}
