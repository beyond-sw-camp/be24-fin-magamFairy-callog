package org.example.backend.adcheck.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

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
            String resultStatus,
            String riskLevel,
            String summaryMessage,
            String mongoDocumentId,
            AdCheckDto.FileCheckRes result,
            String targetUrl,
            Date createdAt,
            Date updatedAt,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {
        public static JobRes from(AdCheckJob job, ObjectMapper objectMapper) {
            AdCheckDto.FileCheckRes result = parseResult(job.getResultPayload(), objectMapper);
            String analysisJobId = firstText(job.getMongoDocumentId(), result == null ? null : result.getAnalysisJobId());
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
                    job.getResultStatus(),
                    job.getRiskLevel(),
                    job.getSummaryMessage(),
                    job.getMongoDocumentId(),
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

    public record JobSummaryRes(
            String jobId,
            Long requesterId,
            String campaignId,
            String fileName,
            String status,
            String resultStatus,
            String riskLevel,
            String summaryMessage,
            String mongoDocumentId,
            String targetUrl,
            Date createdAt,
            Date updatedAt,
            LocalDateTime finishedAt
    ) {
        public static JobSummaryRes from(AdCheckJob job, ObjectMapper objectMapper) {
            AdCheckDto.FileCheckRes result = JobRes.parseResult(job.getResultPayload(), objectMapper);
            String documentId = firstText(job.getMongoDocumentId(), result == null ? null : result.getAnalysisJobId());
            String targetUrl = documentId == null || documentId.isBlank()
                    ? null
                    : "/references?analysisJobId=" + documentId;

            return new JobSummaryRes(
                    job.getJobId(),
                    job.getRequester() == null ? null : job.getRequester().getIdx(),
                    job.getCampaignId(),
                    job.getFileName(),
                    job.getStatus().name(),
                    firstText(job.getResultStatus(), result == null ? null : result.getStatus()),
                    job.getRiskLevel(),
                    job.getSummaryMessage(),
                    documentId,
                    targetUrl,
                    job.getCreatedAt(),
                    job.getUpdatedAt(),
                    job.getFinishedAt()
            );
        }
    }

    public record JobDetailRes(
            JobSummaryRes summary,
            String mongoDocumentId,
            AdCheckDto.FileCheckRes detail,
            Map<String, Object> rawDocument
    ) {
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
