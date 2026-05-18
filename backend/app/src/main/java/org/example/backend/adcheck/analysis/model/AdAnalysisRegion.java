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
        name = "ad_analysis_region",
        indexes = {
                @Index(name = "idx_ad_analysis_region_page", columnList = "page_idx,order_index"),
                @Index(name = "idx_ad_analysis_region_type", columnList = "region_type"),
                @Index(name = "idx_ad_analysis_region_key", columnList = "region_key")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AdAnalysisRegion extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_idx", nullable = false)
    private AdAnalysisPage page;

    @Column(nullable = false, length = 40)
    private String regionKey;

    @Column(name = "region_type", nullable = false, length = 30)
    private String regionType;

    @Column(nullable = false)
    private Integer orderIndex;

    private Double x;

    private Double y;

    private Double width;

    private Double height;

    private Double confidence;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String extractedText;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String labelsJson;
}
