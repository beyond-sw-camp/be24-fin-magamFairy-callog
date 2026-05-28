package org.example.backend.adcheck.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.common.model.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ad_ai_analysis",
        indexes = {
                @Index(name = "idx_ad_ai_analysis_campaign", columnList = "campaign_idx"),
                @Index(name = "idx_ad_ai_analysis_author", columnList = "author_idx"),
                @Index(name = "idx_ad_ai_analysis_status", columnList = "analysis_status")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ad_ai_analysis_job",
                columnNames = "analysis_job_id"
        )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AdAiAnalysis extends BaseEntity {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(name = "analysis_job_id", nullable = false, length = 255)
    private String analysisJobId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_idx", nullable = false)
    private Campaign campaign;

    @Column(name = "author_idx", nullable = false)
    private Long authorIdx;

    @Column(nullable = false, length = 100)
    private String authorLoginId;

    @Column(nullable = false, length = 100)
    private String authorName;

    @Column(length = 255)
    private String fileName;

    @Column(length = 700)
    private String fileObjectKey;

    @Column(length = 100)
    private String fileContentType;

    private Long fileSize;

    @Column(length = 30)
    private String aiStatus;

    @Column(name = "analysis_status", nullable = false, length = 30)
    private String analysisStatus;

    @Column(length = 1000)
    private String errorMessage;

    private LocalDateTime completedAt;

    public void acceptResponse(AdCheckDto.FileCheckRes response) {
        if (response == null) {
            return;
        }
        this.fileName = choose(response.getFileName(), this.fileName);
        this.fileObjectKey = choose(response.getFileObjectKey(), this.fileObjectKey);
        this.fileContentType = choose(response.getFileContentType(), this.fileContentType);
        this.fileSize = response.getFileSize() != null ? response.getFileSize() : this.fileSize;
        this.aiStatus = choose(response.getStatus(), this.aiStatus);
        this.errorMessage = choose(response.getErrorMessage(), this.errorMessage);
    }

    public void completeFromEvent(
            String fileName,
            String fileObjectKey,
            String fileContentType,
            Long fileSize,
            String aiStatus
    ) {
        this.fileName = choose(fileName, this.fileName);
        this.fileObjectKey = choose(fileObjectKey, this.fileObjectKey);
        this.fileContentType = choose(fileContentType, this.fileContentType);
        this.fileSize = fileSize != null ? fileSize : this.fileSize;
        this.aiStatus = choose(aiStatus, this.aiStatus);
        this.analysisStatus = STATUS_COMPLETED;
        this.errorMessage = null;
        this.completedAt = LocalDateTime.now();
    }

    public void failFromEvent(
            String fileName,
            String fileObjectKey,
            String fileContentType,
            Long fileSize,
            String aiStatus,
            String errorMessage
    ) {
        this.fileName = choose(fileName, this.fileName);
        this.fileObjectKey = choose(fileObjectKey, this.fileObjectKey);
        this.fileContentType = choose(fileContentType, this.fileContentType);
        this.fileSize = fileSize != null ? fileSize : this.fileSize;
        this.aiStatus = choose(aiStatus, this.aiStatus);
        this.analysisStatus = STATUS_FAILED;
        this.errorMessage = choose(errorMessage, this.errorMessage);
        this.completedAt = LocalDateTime.now();
    }

    public void failRequest(String errorMessage) {
        this.analysisStatus = STATUS_FAILED;
        this.errorMessage = choose(errorMessage, this.errorMessage);
    }

    private String choose(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
