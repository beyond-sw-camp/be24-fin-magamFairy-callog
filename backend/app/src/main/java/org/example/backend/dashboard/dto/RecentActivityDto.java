package org.example.backend.dashboard.dto;

import java.time.LocalDateTime;

/** Zone1 P1 우측 "최근 활동" 피드 항목. */
public record RecentActivityDto(
        Long idx,
        Long campaignId,
        String campaignName,
        String type,
        String description,
        String actorName,
        LocalDateTime createdAt
) {}
