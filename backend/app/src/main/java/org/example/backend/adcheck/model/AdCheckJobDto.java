package org.example.backend.adcheck.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Date;

public class AdCheckJobDto {
    public record ProgressReq(
            String jobId,
            String token,
            String step
    ) {
    }

    public record JobRes(
            String jobId,
            Long requesterId,
            String campaignId,
            String fileName,
            String status,
            String currentStep,
            String currentStepLabel,
            String currentStepMessage,
            Integer currentStepOrder,
            Integer progressPercent,
            String errorMessage,
            AdCheckDto.FileCheckRes result,
            String targetUrl,
            Date createdAt,
            Date updatedAt,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {
        public static JobRes from(AdCheckJob job, ObjectMapper objectMapper) {
            AdCheckDto.FileCheckRes result = parseResult(job.getResultPayload(), objectMapper);
            String analysisJobId = result == null ? null : result.getAnalysisJobId();
            String targetUrl = analysisJobId == null || analysisJobId.isBlank()
                    ? null
                    : "/references?analysisJobId=" + analysisJobId;

            return new JobRes(
                    job.getJobId(),
                    job.getRequester() == null ? null : job.getRequester().getIdx(),
                    job.getCampaignId(),
                    job.getFileName(),
                    job.getStatus().name(),
                    job.getCurrentStep().name(),
                    job.getCurrentStep().getLabel(),
                    job.getCurrentStep().getMessage(),
                    job.getCurrentStep().getOrder(),
                    job.getProgressPercent(),
                    job.getErrorMessage(),
                    result,
                    targetUrl,
                    job.getCreatedAt(),
                    job.getUpdatedAt(),
                    job.getStartedAt(),
                    job.getFinishedAt()
            );
        }

        private static AdCheckDto.FileCheckRes parseResult(String resultPayload, ObjectMapper objectMapper) {
            if (resultPayload == null || resultPayload.isBlank()) {
                return null;
            }

            try {
                return objectMapper.readValue(resultPayload, AdCheckDto.FileCheckRes.class);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
