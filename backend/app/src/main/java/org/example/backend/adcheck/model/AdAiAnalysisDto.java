package org.example.backend.adcheck.model;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Date;

public class AdAiAnalysisDto {
    @Builder
    public record SummaryRes(
            Long idx,
            String analysisJobId,
            Long campaignIdx,
            String campaignName,
            Long authorIdx,
            String authorLoginId,
            String authorName,
            String fileName,
            String fileObjectKey,
            String fileContentType,
            Long fileSize,
            String aiStatus,
            String analysisStatus,
            String errorMessage,
            LocalDateTime completedAt,
            Date createdAt
    ) {
        public static SummaryRes from(AdAiAnalysis entity) {
            return SummaryRes.builder()
                    .idx(entity.getIdx())
                    .analysisJobId(entity.getAnalysisJobId())
                    .campaignIdx(entity.getCampaign() != null ? entity.getCampaign().getIdx() : null)
                    .campaignName(entity.getCampaign() != null ? entity.getCampaign().getName() : null)
                    .authorIdx(entity.getAuthorIdx())
                    .authorLoginId(entity.getAuthorLoginId())
                    .authorName(entity.getAuthorName())
                    .fileName(entity.getFileName())
                    .fileObjectKey(entity.getFileObjectKey())
                    .fileContentType(entity.getFileContentType())
                    .fileSize(entity.getFileSize())
                    .aiStatus(entity.getAiStatus())
                    .analysisStatus(entity.getAnalysisStatus())
                    .errorMessage(entity.getErrorMessage())
                    .completedAt(entity.getCompletedAt())
                    .createdAt(entity.getCreatedAt())
                    .build();
        }
    }

    public record DetailRes(
            SummaryRes summary,
            AdCheckDto.FileCheckRes detail,
            boolean detailAvailable,
            String detailMessage
    ) {
        public static DetailRes available(AdAiAnalysis analysis, AdCheckDto.FileCheckRes detail) {
            return new DetailRes(SummaryRes.from(analysis), detail, true, null);
        }

        public static DetailRes unavailable(AdAiAnalysis analysis, String detailMessage) {
            return new DetailRes(SummaryRes.from(analysis), null, false, detailMessage);
        }
    }
}
