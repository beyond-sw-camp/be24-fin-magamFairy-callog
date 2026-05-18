package org.example.backend.kpi.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.backend.campaign.model.Campaign;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "campaign_kpi_contribution",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_campaign_target_org_kpi",
                columnNames = {"campaign_id", "target_org_kpi_id"}),
        indexes = {
                @Index(name = "idx_contrib_target_org_kpi", columnList = "target_org_kpi_id"),
                @Index(name = "idx_contrib_campaign", columnList = "campaign_id")
        })
public class CampaignKpiContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_org_kpi_id", nullable = false)
    private OrganizationKpi targetOrgKpi;

    @Setter
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal committedValue;

    @Setter
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal actualValue = BigDecimal.ZERO;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.actualValue == null) {
            this.actualValue = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
