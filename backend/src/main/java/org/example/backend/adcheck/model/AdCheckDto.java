package org.example.backend.adcheck.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
        private String extractedText;
        private String status;
        private String law;
        private String violationText;
        private String reason;
        private String suggestion;

        public static FileCheckRes of(String fileName, String extractedText, Res res) {
            return FileCheckRes.builder()
                    .fileName(fileName)
                    .extractedText(extractedText)
                    .status(res.getStatus())
                    .law(res.getLaw())
                    .violationText(res.getViolationText())
                    .reason(res.getReason())
                    .suggestion(res.getSuggestion())
                    .build();
        }
    }
}
