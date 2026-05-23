package org.example.backend.adcheck.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.common.model.BaseEntity;
import org.example.backend.user.model.User;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ad_check_job",
        indexes = {
                @Index(name = "idx_ad_check_job_job_id", columnList = "job_id", unique = true),
                @Index(name = "idx_ad_check_job_requester_status", columnList = "requester_idx,status")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AdCheckJob extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(name = "job_id", nullable = false, unique = true, length = 80)
    private String jobId;

    @Column(name = "progress_token", length = 80)
    private String progressToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_idx", nullable = false)
    private User requester;

    @Column(length = 80)
    private String campaignId;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(length = 100)
    private String fileContentType;

    private Long fileSize;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] fileBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdCheckJobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AdCheckJobStep currentStep;

    @Column(nullable = false)
    private Integer progressPercent;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(length = 30)
    private String resultStatus;

    @Column(length = 20)
    private String riskLevel;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String summaryMessage;

    @Column(length = 120)
    private String mongoDocumentId;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String resultPayload;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public static AdCheckJob queued(
            String jobId,
            String progressToken,
            User requester,
            String campaignId,
            String fileName,
            String fileContentType,
            Long fileSize,
            byte[] fileBytes
    ) {
        return AdCheckJob.builder()
                .jobId(jobId)
                .progressToken(trimToDefault(progressToken, jobId))
                .requester(requester)
                .campaignId(trimToNull(campaignId))
                .fileName(trimToDefault(fileName, "upload"))
                .fileContentType(trimToNull(fileContentType))
                .fileSize(fileSize)
                .fileBytes(fileBytes)
                .status(AdCheckJobStatus.QUEUED)
                .currentStep(AdCheckJobStep.QUEUED)
                .progressPercent(AdCheckJobStep.QUEUED.getProgressPercent())
                .build();
    }

    public void markQueued() {
        if (isTerminal()) {
            return;
        }
        this.status = AdCheckJobStatus.QUEUED;
        changeStep(AdCheckJobStep.QUEUED);
    }

    public void markRunning() {
        if (isTerminal()) {
            return;
        }
        this.status = AdCheckJobStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
        changeStep(AdCheckJobStep.DATA_EXTRACTION);
    }

    public void changeStep(AdCheckJobStep step) {
        if (step == null || isTerminal()) {
            return;
        }
        this.currentStep = step;
        if (step != AdCheckJobStep.FAILED) {
            this.progressPercent = step.getProgressPercent();
        }
    }

    public void markSucceeded(
            String resultPayload,
            String mongoDocumentId,
            String resultStatus,
            String riskLevel,
            String summaryMessage
    ) {
        this.status = AdCheckJobStatus.SUCCEEDED;
        this.currentStep = AdCheckJobStep.COMPLETED;
        this.progressPercent = AdCheckJobStep.COMPLETED.getProgressPercent();
        this.resultPayload = resultPayload;
        this.mongoDocumentId = trimToNull(mongoDocumentId);
        this.resultStatus = trimToNull(resultStatus);
        this.riskLevel = trimToNull(riskLevel);
        this.summaryMessage = trimToNull(summaryMessage);
        this.errorMessage = null;
        this.finishedAt = LocalDateTime.now();
        clearFileBytes();
    }

    public void markFailed(String errorMessage, String resultPayload) {
        markFailed(errorMessage, resultPayload, null, null, "HIGH", errorMessage);
    }

    public void markFailed(
            String errorMessage,
            String resultPayload,
            String mongoDocumentId,
            String resultStatus,
            String riskLevel,
            String summaryMessage
    ) {
        this.status = AdCheckJobStatus.FAILED;
        this.currentStep = AdCheckJobStep.FAILED;
        this.errorMessage = errorMessage;
        this.mongoDocumentId = trimToNull(mongoDocumentId);
        this.resultStatus = trimToNull(resultStatus);
        this.riskLevel = trimToNull(riskLevel);
        this.summaryMessage = trimToNull(summaryMessage);
        if (resultPayload != null && !resultPayload.isBlank()) {
            this.resultPayload = resultPayload;
        }
        this.finishedAt = LocalDateTime.now();
        clearFileBytes();
    }

    public void cancel() {
        if (status != AdCheckJobStatus.QUEUED) {
            return;
        }
        this.status = AdCheckJobStatus.CANCELED;
        this.errorMessage = "대기 중 검수 작업이 취소되었습니다.";
        this.finishedAt = LocalDateTime.now();
        clearFileBytes();
    }

    public boolean isTerminal() {
        return status == AdCheckJobStatus.SUCCEEDED
                || status == AdCheckJobStatus.FAILED
                || status == AdCheckJobStatus.CANCELED;
    }

    public void clearFileBytes() {
        this.fileBytes = null;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String trimToDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
