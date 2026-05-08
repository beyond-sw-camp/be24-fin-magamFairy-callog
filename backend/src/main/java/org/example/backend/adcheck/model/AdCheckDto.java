package org.example.backend.adcheck.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

public class AdCheckDto {

    @Getter
    @Builder
    public static class Req {
        private String copy;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Res {
        private String status;          // violation | warning | pass
        private String law;
        @JsonProperty("violation_text")
        private String violationText;
        private String reason;
        private String suggestion;
    }

    @Getter
    @Builder
    public static class FileCheckRes {
        private String fileName;
        private String fileObjectKey;
        private String fileUrl;
        private String fileContentType;
        private Long fileSize;
        private String extractedText;
        private String status;
        private String law;
        private String violationText;
        private String reason;
        private String suggestion;

        public static FileCheckRes of(String fileName, String extractedText, Res res) {
            return of(fileName, null, null, null, null, extractedText, res);
        }

        public static FileCheckRes of(
                String fileName,
                String fileObjectKey,
                String fileUrl,
                String fileContentType,
                Long fileSize,
                String extractedText,
                Res res
        ) {
            return FileCheckRes.builder()
                    .fileName(fileName)
                    .fileObjectKey(fileObjectKey)
                    .fileUrl(fileUrl)
                    .fileContentType(fileContentType)
                    .fileSize(fileSize)
                    .extractedText(extractedText)
                    .status(res.getStatus())
                    .law(res.getLaw())
                    .violationText(res.getViolationText())
                    .reason(res.getReason())
                    .suggestion(res.getSuggestion())
                    .build();
        }
    }

    public record ReviewRequestCreateReq(
            String fileName,
            String fileObjectKey,
            String fileContentType,
            Long fileSize,
            String extractedText,
            String status,
            String law,
            String violationText,
            String reason,
            String suggestion,
            String requestMemo
    ) {
    }

    public record ReviewDecisionReq(
            String memo,
            String reason
    ) {
    }

    @Builder
    public record ReviewRequestRes(
            Long idx,
            Long campaignIdx,
            String campaignName,
            String fileName,
            String fileObjectKey,
            String fileUrl,
            String fileContentType,
            Long fileSize,
            String extractedText,
            String requestStatus,
            String aiStatus,
            String law,
            String violationText,
            String reason,
            String suggestion,
            String requestMemo,
            String requesterLoginId,
            String requesterName,
            Long requesterOrganizationIdx,
            String requesterOrganizationName,
            String reviewerLoginId,
            String reviewerName,
            String reviewedAt,
            String reviewMemo,
            String rejectReason,
            Date createdAt
    ) {
        public static ReviewRequestRes from(AdReviewRequest entity, String fileUrl) {
            return ReviewRequestRes.builder()
                    .idx(entity.getIdx())
                    .campaignIdx(entity.getCampaign() != null ? entity.getCampaign().getIdx() : null)
                    .campaignName(entity.getCampaign() != null ? entity.getCampaign().getName() : null)
                    .fileName(entity.getFileName())
                    .fileObjectKey(entity.getFileObjectKey())
                    .fileUrl(fileUrl)
                    .fileContentType(entity.getFileContentType())
                    .fileSize(entity.getFileSize())
                    .extractedText(entity.getExtractedText())
                    .requestStatus(entity.getRequestStatus())
                    .aiStatus(entity.getAiStatus())
                    .law(entity.getLaw())
                    .violationText(entity.getViolationText())
                    .reason(entity.getReason())
                    .suggestion(entity.getSuggestion())
                    .requestMemo(entity.getRequestMemo())
                    .requesterLoginId(entity.getRequesterLoginId())
                    .requesterName(entity.getRequesterName())
                    .requesterOrganizationIdx(entity.getRequesterOrganizationIdx())
                    .requesterOrganizationName(entity.getRequesterOrganizationName())
                    .reviewerLoginId(entity.getReviewerLoginId())
                    .reviewerName(entity.getReviewerName())
                    .reviewedAt(entity.getReviewedAt() != null ? entity.getReviewedAt().toString() : null)
                    .reviewMemo(entity.getReviewMemo())
                    .rejectReason(entity.getRejectReason())
                    .createdAt(entity.getCreatedAt())
                    .build();
        }
    }
}
