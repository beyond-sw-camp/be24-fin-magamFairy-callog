package org.example.backend.dashboard.dto;

/** Zone2 캠페인 진척률 랭킹 항목. */
public record CampaignProgressDto(
        Long campaignId,
        String campaignName,
        String color,
        boolean isMine,
        int completionPct
) {}
