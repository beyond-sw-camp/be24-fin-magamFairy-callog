package org.example.backend.adcheck.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.common.model.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "ad_review_request")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AdReviewRequest extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_idx")
    private Campaign campaign;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 700)
    private String fileObjectKey;

    @Column(length = 100)
    private String fileContentType;

    private Long fileSize;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String extractedText;

    @Column(nullable = false, length = 30)
    private String requestStatus;

    @Column(nullable = false, length = 30)
    private String aiStatus;

    @Column(length = 255)
    private String law;

    @Column(length = 1000)
    private String violationText;

    @Column(length = 2000)
    private String reason;

    @Column(length = 2000)
    private String suggestion;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String requestMemo;

    @Column(length = 100)
    private String requesterLoginId;

    @Column(length = 100)
    private String requesterName;

    private Long requesterOrganizationIdx;

    @Column(length = 150)
    private String requesterOrganizationName;

    @Column(length = 100)
    private String reviewerLoginId;

    @Column(length = 100)
    private String reviewerName;

    private LocalDateTime reviewedAt;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String reviewMemo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String rejectReason;

    public void approve(String reviewerLoginId, String reviewerName, String reviewMemo) {
        this.requestStatus = "APPROVED";
        this.reviewerLoginId = reviewerLoginId;
        this.reviewerName = reviewerName;
        this.reviewedAt = LocalDateTime.now();
        this.reviewMemo = reviewMemo;
        this.rejectReason = null;
    }

    public void reject(String reviewerLoginId, String reviewerName, String rejectReason) {
        this.requestStatus = "REJECTED";
        this.reviewerLoginId = reviewerLoginId;
        this.reviewerName = reviewerName;
        this.reviewedAt = LocalDateTime.now();
        this.rejectReason = rejectReason;
    }
}
