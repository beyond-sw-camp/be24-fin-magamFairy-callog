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
            String requesterLoginId,
            String requesterName,
            String requesterOrganizationName,
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
            Integer verdictLevel,
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
            String targetUrl = buildTargetUrl(job, analysisJobId);

            return new JobRes(
                    job.getJobId(),
                    job.getRequester() == null ? null : job.getRequester().getIdx(),
                    job.getRequester() == null ? null : job.getRequester().getId(),
                    job.getRequester() == null ? null : job.getRequester().getName(),
                    job.getRequester() == null || job.getRequester().getOrganization() == null
                            ? null
                            : job.getRequester().getOrganization().getName(),
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
                    firstVerdictLevel(result == null ? null : result.getVerdictLevel(), job.getRiskLevel()),
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
            String requesterLoginId,
            String requesterName,
            String requesterOrganizationName,
            String campaignId,
            String fileName,
            String status,
            String resultStatus,
            String riskLevel,
            Integer verdictLevel,
            String summaryMessage,
            String mongoDocumentId,
            String fileUrl,
            String fileContentType,
            Long fileSize,
            String thumbnailUrl,
            String targetUrl,
            Date createdAt,
            Date updatedAt,
            LocalDateTime finishedAt
    ) {
        public JobSummaryRes withStorageUrls(String nextFileUrl, String nextThumbnailUrl) {
            return new JobSummaryRes(
                    jobId,
                    requesterId,
                    requesterLoginId,
                    requesterName,
                    requesterOrganizationName,
                    campaignId,
                    fileName,
                    status,
                    resultStatus,
                    riskLevel,
                    verdictLevel,
                    summaryMessage,
                    mongoDocumentId,
                    firstText(nextFileUrl, fileUrl),
                    fileContentType,
                    fileSize,
                    firstText(nextThumbnailUrl, thumbnailUrl),
                    targetUrl,
                    createdAt,
                    updatedAt,
                    finishedAt
            );
        }

        public static JobSummaryRes from(AdCheckJob job, ObjectMapper objectMapper) {
            AdCheckDto.FileCheckRes result = JobRes.parseResult(job.getResultPayload(), objectMapper);
            String documentId = firstText(job.getMongoDocumentId(), result == null ? null : result.getAnalysisJobId());
            String targetUrl = buildTargetUrl(job, documentId);

            return new JobSummaryRes(
                    job.getJobId(),
                    job.getRequester() == null ? null : job.getRequester().getIdx(),
                    job.getRequester() == null ? null : job.getRequester().getId(),
                    job.getRequester() == null ? null : job.getRequester().getName(),
                    job.getRequester() == null || job.getRequester().getOrganization() == null
                            ? null
                            : job.getRequester().getOrganization().getName(),
                    job.getCampaignId(),
                    job.getFileName(),
                    job.getStatus().name(),
                    firstText(job.getResultStatus(), result == null ? null : result.getStatus()),
                    job.getRiskLevel(),
                    firstVerdictLevel(result == null ? null : result.getVerdictLevel(), job.getRiskLevel()),
                    job.getSummaryMessage(),
                    documentId,
                    result == null ? null : result.getFileUrl(),
                    result == null ? null : result.getFileContentType(),
                    result == null ? null : result.getFileSize(),
                    resolveThumbnailUrl(result),
                    targetUrl,
                    job.getCreatedAt(),
                    job.getUpdatedAt(),
                    job.getFinishedAt()
            );
        }
    }

    private static String resolveThumbnailUrl(AdCheckDto.FileCheckRes result) {
        if (result == null) {
            return null;
        }
        if (result.getFileContentType() != null && result.getFileContentType().startsWith("image/")) {
            return result.getFileUrl();
        }
        if (result.getExtractedImageAssets() != null) {
            for (AdCheckDto.FileArtifact artifact : result.getExtractedImageAssets()) {
                if (artifact != null && artifact.getUrl() != null && !artifact.getUrl().isBlank()) {
                    return artifact.getUrl();
                }
            }
        }
        return null;
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

    private static String buildTargetUrl(AdCheckJob job, String analysisJobId) {
        if (job.getCampaignId() != null && !job.getCampaignId().isBlank()) {
            return "/campaigns/" + job.getCampaignId() + "?tab=review&adCheckJobId=" + job.getJobId();
        }
        if (analysisJobId != null && !analysisJobId.isBlank()) {
            return "/references?analysisJobId=" + analysisJobId;
        }
        return null;
    }

    private static Integer firstVerdictLevel(Integer resultLevel, String storedRiskLevel) {
        if (resultLevel != null && resultLevel >= 1 && resultLevel <= 5) {
            return resultLevel;
        }
        if (storedRiskLevel == null || storedRiskLevel.isBlank()) {
            return null;
        }
        for (int index = 0; index < storedRiskLevel.length(); index++) {
            char current = storedRiskLevel.charAt(index);
            if (current >= '1' && current <= '5') {
                return current - '0';
            }
        }
        return null;
    }
}
