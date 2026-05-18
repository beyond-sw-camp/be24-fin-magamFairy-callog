package org.example.backend.campaignframe.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

public class CampaignFrameDto {
    public record CreateFrameReq(
            String id,
            String category,
            String version,
            String title,
            Integer score,
            String status,
            String overview,
            @JsonProperty("required_fields") List<String> requiredFields,
            @JsonProperty("banned_expressions") List<String> bannedExpressions,
            @JsonProperty("recommended_expressions") List<String> recommendedExpressions,
            @JsonProperty("tone_guide") String toneGuide,
            @JsonProperty("approval_process") List<String> approvalProcess,
            PerformanceReq performance
    ) {
    }

    public record UpdateFrameReq(
            String category,
            String version,
            String title,
            Integer score,
            String status,
            String overview,
            @JsonProperty("required_fields") List<String> requiredFields,
            @JsonProperty("banned_expressions") List<String> bannedExpressions,
            @JsonProperty("recommended_expressions") List<String> recommendedExpressions,
            @JsonProperty("tone_guide") String toneGuide,
            @JsonProperty("approval_process") List<String> approvalProcess,
            PerformanceReq performance
    ) {
    }

    public record PerformanceReq(
            @JsonProperty("usage_count") Integer usageCount,
            @JsonProperty("pass_rate") Integer passRate,
            @JsonProperty("avg_revisions") Double avgRevisions
    ) {
    }

    @Builder
    public record UpsertReq(
            String id,
            String category,
            String version,
            String title,
            Integer score,
            String status,
            String overview,
            List<String> requiredFields,
            List<String> bannedExpressions,
            List<String> recommendedExpressions,
            String toneGuide,
            List<String> approvalProcess,
            PerformanceReq performance
    ) {
    }

    @Builder
    public record FrameRes(
            Long idx,
            String id,
            String category,
            String version,
            String title,
            Integer score,
            String status,
            String overview,
            @JsonProperty("required_fields") List<String> requiredFields,
            @JsonProperty("banned_expressions") List<String> bannedExpressions,
            @JsonProperty("recommended_expressions") List<String> recommendedExpressions,
            @JsonProperty("tone_guide") String toneGuide,
            @JsonProperty("approval_process") List<String> approvalProcess,
            PerformanceRes performance
    ) {
        public static FrameRes from(CampaignFrame entity) {
            return FrameRes.builder()
                    .idx(entity.getIdx())
                    .id(entity.getId())
                    .category(entity.getCategory())
                    .version(entity.getVersion())
                    .title(entity.getTitle())
                    .score(entity.getScore())
                    .status(entity.getStatus())
                    .overview(entity.getOverview())
                    .requiredFields(List.copyOf(entity.getRequiredFields()))
                    .bannedExpressions(List.copyOf(entity.getBannedExpressions()))
                    .recommendedExpressions(List.copyOf(entity.getRecommendedExpressions()))
                    .toneGuide(entity.getToneGuide())
                    .approvalProcess(List.copyOf(entity.getApprovalProcess()))
                    .performance(PerformanceRes.from(entity))
                    .build();
        }
    }

    @Builder
    public record PerformanceRes(
            @JsonProperty("usage_count") Integer usageCount,
            @JsonProperty("pass_rate") Integer passRate,
            @JsonProperty("avg_revisions") Double avgRevisions
    ) {
        public static PerformanceRes from(CampaignFrame entity) {
            return PerformanceRes.builder()
                    .usageCount(entity.getUsageCount())
                    .passRate(entity.getPassRate())
                    .avgRevisions(entity.getAvgRevisions())
                    .build();
        }
    }

    @Builder
    public record DeleteFrameRes(
            String id,
            String title,
            Boolean deleted
    ) {
        public static DeleteFrameRes from(CampaignFrame entity) {
            return DeleteFrameRes.builder()
                    .id(entity.getId())
                    .title(entity.getTitle())
                    .deleted(true)
                    .build();
        }
    }
}
