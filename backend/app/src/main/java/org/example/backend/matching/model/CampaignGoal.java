package org.example.backend.matching.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.backend.organization.model.Organization;
import org.example.backend.user.model.User;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "campaign_goal")
public class CampaignGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    private String name;

    @Enumerated(EnumType.STRING)
    private GoalType primaryType;

    @Enumerated(EnumType.STRING)
    private GoalType secondaryType;

    private String kpiPrimary;
    private String kpiSecondary;

    private String budgetLimit;
    private String effortLimit;

    private LocalDate periodStart;
    private LocalDate periodEnd;

    private Integer weightRevenue;
    private Integer weightEffort;
    private Integer weightBrand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_idx")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_organization_idx")
    private Organization ownerOrganization;

    private String ownerLabel;
    private String status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void update(MatchingDto.AddGoal dto) {
        this.name = dto.getName();
        this.primaryType = dto.getPrimaryType();
        this.secondaryType = dto.getSecondaryType();
        this.kpiPrimary = dto.getKpiPrimary();
        this.kpiSecondary = dto.getKpiSecondary();
        this.budgetLimit = dto.getBudgetLimit();
        this.effortLimit = dto.getEffortLimit();
        this.periodStart = dto.getPeriodStart();
        this.periodEnd = dto.getPeriodEnd();
        this.weightRevenue = dto.getWeightRevenue();
        this.weightEffort = dto.getWeightEffort();
        this.weightBrand = dto.getWeightBrand();
        this.ownerLabel = dto.getOwnerLabel();
        this.status = dto.getStatus();
    }
}
