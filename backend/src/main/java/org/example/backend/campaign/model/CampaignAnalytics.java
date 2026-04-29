package org.example.backend.campaign.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.common.model.BaseEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "campaign_analytics")
public class CampaignAnalytics extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false, unique = true)
    private Campaign campaign;

    @Builder.Default
    @Column(nullable = false)
    private Long impressions = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long clicks = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long conversions = 0L;

    @Builder.Default
    @Column(precision = 7, scale = 2)
    private BigDecimal clickThroughRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 7, scale = 2)
    private BigDecimal conversionRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 15, scale = 2)
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 15, scale = 2)
    private BigDecimal costPerClick = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 15, scale = 2)
    private BigDecimal returnOnAdSpend = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private Integer participantCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer completionCount = 0;

    public void updateMetrics(Long impressions, Long clicks, Long conversions, BigDecimal totalSpent) {
        this.impressions = safeLong(impressions);
        this.clicks = safeLong(clicks);
        this.conversions = safeLong(conversions);
        this.totalSpent = totalSpent == null ? BigDecimal.ZERO : totalSpent;
        recalculateRates();
    }

    public void updateProgress(Integer participantCount, Integer completionCount) {
        this.participantCount = participantCount == null ? 0 : participantCount;
        this.completionCount = completionCount == null ? 0 : completionCount;
    }

    private void recalculateRates() {
        this.clickThroughRate = impressions == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(clicks)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(impressions), 2, RoundingMode.HALF_UP);
        this.conversionRate = clicks == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(conversions)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(clicks), 2, RoundingMode.HALF_UP);
        this.costPerClick = clicks == 0
                ? BigDecimal.ZERO
                : totalSpent.divide(BigDecimal.valueOf(clicks), 2, RoundingMode.HALF_UP);
    }

    private static Long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
