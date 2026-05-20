package org.example.backend.dashboard.dto;

/** Zone4 P1 캠페인 파이프라인 퍼널의 한 단계. */
public record PipelineStageDto(
        String stage,
        long count
) {}
