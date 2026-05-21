package org.example.backend.dashboard.dto;

import java.util.List;

/**
 * Zone3 P2 검수 — 두 관점 동시 제공(토글).
 * toReview = 내가 만든/참여 캠페인에 들어온 '내가 검수할' 요청(REQUESTED, 남이 제출) → 승인/반려 대상.
 * mine     = 우리 조직이 '제출한' 검수 요청 전체(승인/반려/대기 상태 포함) → 결과 확인용.
 */
public record AdReviewQueueDto(
        List<AdReviewQueueItemDto> toReview,
        List<AdReviewQueueItemDto> mine
) {}
