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
        name = "ad_analysis_page",
        indexes = {
                @Index(name = "idx_ad_analysis_page_document", columnList = "document_idx,page_no")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AdAnalysisPage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_idx", nullable = false)
    private AdAnalysisDocument document;

    @Column(nullable = false)
    private Integer pageNo;

    private Integer width;

    private Integer height;

    @Column(length = 700)
    private String thumbnailObjectKey;
}
