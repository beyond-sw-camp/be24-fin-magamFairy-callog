package org.example.backend.dashboard.dto;

/**
 * Zone3 P2 검수 — 광고검수 요청(AdReviewRequest) 큐 항목.
 * requestId + campaignId 로 승인/반려 PATCH 호출 가능.
 */
public record AdReviewQueueItemDto(
        Long requestId,
        Long campaignId,
        String campaignName,
        String fileName,
        String requesterName,
        String requestStatus
) {}
