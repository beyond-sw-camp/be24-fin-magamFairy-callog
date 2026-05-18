package org.example.backend.campaign.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.backend.common.model.BaseEntity;
import org.example.backend.kpi.model.EsgCategory;
import org.example.backend.user.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "campaign_kpis")
public class CampaignKpi extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_idx", nullable = false)
    private Campaign campaign;

    @Setter
    @Column(nullable = false, length = 120)
    private String name;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private KpiCategory category;

    @Setter
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal targetValue;

    @Setter
    @Column(precision = 18, scale = 4)
    private BigDecimal actualValue;

    @Setter
    @Column(nullable = false, length = 20)
    private String unit;

    @Setter
    @Column(length = 120)
    private String ownerLabel;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_idx")
    private User ownerUser;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String memo;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String nextAction;

    @Setter
    private LocalDateTime measuredAt;

    /**
     * 옵션: 어떤 상위 OrganizationKpi에 cascade되는가 (기여 매핑은 CampaignKpiContribution에 별도 저장).
     */
    @Setter
    @Column(name = "parent_org_kpi_id")
    private Long parentOrgKpiId;

    /**
     * KpiCategory에서 ESG가 분리된 후 ESG KPI를 표현하기 위한 보조 필드.
     * null이면 일반 KPI, 값이 있으면 ESG KPI (E/S/G 분류).
     */
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "esg_category", length = 30)
    private EsgCategory esgCategory;
}
