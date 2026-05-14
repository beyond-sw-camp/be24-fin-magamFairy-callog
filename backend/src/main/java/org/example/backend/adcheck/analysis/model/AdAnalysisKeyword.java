package org.example.backend.adcheck.analysis.model;

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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.common.model.BaseEntity;

@Entity
@Table(
        name = "ad_analysis_keyword",
        indexes = {
                @Index(name = "idx_ad_analysis_keyword_document", columnList = "document_idx"),
                @Index(name = "idx_ad_analysis_keyword_value", columnList = "keyword")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AdAnalysisKeyword extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_idx", nullable = false)
    private AdAnalysisDocument document;

    @Column(nullable = false, length = 120)
    private String keyword;

    @Column(nullable = false, length = 30)
    private String source;

    private Double weight;
}
