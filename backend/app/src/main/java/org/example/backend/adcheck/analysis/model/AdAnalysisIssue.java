package org.example.backend.adcheck.analysis.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(
        name = "ad_analysis_issue",
        indexes = {
                @Index(name = "idx_ad_analysis_issue_document", columnList = "document_idx,page_no"),
                @Index(name = "idx_ad_analysis_issue_region", columnList = "region_key"),
                @Index(name = "idx_ad_analysis_issue_severity", columnList = "severity")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AdAnalysisIssue extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_idx", nullable = false)
    private AdAnalysisDocument document;

    @Column(nullable = false)
    private Integer pageNo;

    @Column(nullable = false, length = 40)
    private String regionKey;

    @Column(nullable = false, length = 30)
    private String issueType;

    @Column(nullable = false, length = 30)
    private String severity;

    @Column(nullable = false, length = 30)
    private String issueStatus;

    private Double x;

    private Double y;

    private Double width;

    private Double height;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String targetText;

    @Column(length = 255)
    private String law;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String reason;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String suggestion;
}
