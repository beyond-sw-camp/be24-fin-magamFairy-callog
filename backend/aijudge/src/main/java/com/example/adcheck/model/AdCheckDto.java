package com.example.adcheck.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class AdCheckDto {

    public record Req(
            String copy
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Res(
            String status,
            String law,
            @JsonProperty("violation_text")
            String violationText,
            String reason,
            String suggestion,
            @JsonAlias({
                    "verdict_level",
                    "reviewLevel",
                    "review_level",
                    "riskLevel",
                    "risk_level",
                    "level",
                    "grade"
            })
            Integer verdictLevel
    ) {
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class FileCheckRes {
        private String analysisJobId;
        private String analysisObjectPrefix;
        private String fileName;
        private String fileObjectKey;
        private String fileUrl;
        private String fileContentType;
        private Long fileSize;
        private String extractedTextObjectKey;
        private String extractedTextUrl;
        private String aiResultObjectKey;
        private String aiResultUrl;
        private String finalResultObjectKey;
        private String finalResultUrl;
        private List<FileArtifact> extractedImageAssets;
        private String extractedText;
        private String status;
        private String law;
        private String violationText;
        private String reason;
        private String suggestion;
        private Integer verdictLevel;
        private String extractionMode;
        private ProcessingTimes processingTimes;
        private String errorMessage;
        private Map<String, Object> context;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileArtifact {
        private String type;
        private String targetId;
        private Integer page;
        private Integer readingOrder;
        private String objectKey;
        private String url;
        private String contentType;
        private Long fileSize;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProcessingTimes {
        private Long textExtractionMillis;
        private Long layoutMillis;
        private Long ocrMillis;
        private Long aiAnalysisMillis;
        private Long totalMillis;
    }
}
