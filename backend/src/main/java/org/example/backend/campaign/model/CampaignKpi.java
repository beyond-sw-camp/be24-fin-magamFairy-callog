package org.example.backend.campaign.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.backend.common.model.BaseEntity;
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
}
