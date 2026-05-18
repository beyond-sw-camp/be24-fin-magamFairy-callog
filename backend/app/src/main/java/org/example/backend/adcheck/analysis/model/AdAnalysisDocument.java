package org.example.backend.adcheck.analysis.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.common.model.BaseEntity;

@Entity
@Table(
        name = "ad_analysis_document",
        indexes = {
                @Index(name = "idx_ad_analysis_document_campaign", columnList = "campaign_idx"),
                @Index(name = "idx_ad_analysis_document_review", columnList = "review_request_idx"),
                @Index(name = "idx_ad_analysis_document_status", columnList = "analysis_status")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AdAnalysisDocument extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    private Long reviewRequestIdx;

    private Long campaignIdx;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 700)
    private String fileObjectKey;

    @Column(name = "analysis_status", nullable = false, length = 30)
    private String analysisStatus;

    private Integer totalPages;

    private Integer totalRegions;

    private Integer totalIssues;

    @Column(length = 120)
    private String layoutModel;

    @Column(length = 120)
    private String ocrModel;

    @Column(length = 120)
    private String detectorModel;

    @Column(length = 120)
    private String llmModel;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String summary;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String rawPayload;
}
