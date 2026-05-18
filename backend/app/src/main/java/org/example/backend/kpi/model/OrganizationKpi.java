package org.example.backend.kpi.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.backend.campaign.model.KpiCategory;
import org.example.backend.organization.model.Organization;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "organization_kpi", indexes = {
        @Index(name = "idx_org_kpi_owner_period_status", columnList = "owner_org_id,period_code,status"),
        @Index(name = "idx_org_kpi_parent", columnList = "parent_kpi_id")
})
public class OrganizationKpi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    // 소유 조직 (HQ 또는 AFFILIATE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_org_id", nullable = false)
    private Organization owner;

    // 상위 KPI (cascade) - 옵션 (자체 목표일 수도 있음)
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_kpi_id")
    private OrganizationKpi parentKpi;

    @Setter
    @Column(precision = 18, scale = 4)
    private BigDecimal contributionToParent;

    @Setter
    @Column(nullable = false, length = 200)
    private String name;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoalPeriodType periodType;

    @Setter
    @Column(name = "period_code", length = 20)
    private String periodCode;

    @Setter
    private LocalDate periodStart;

    @Setter
    private LocalDate periodEnd;

    @Setter
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal targetValue;

    @Setter
    @Column(precision = 18, scale = 4)
    private BigDecimal actualValue;

    @Setter
    @Column(nullable = false, length = 50)
    private String unit;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private KpiCategory category;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EsgCategory esgCategory;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private GoalKind kind;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoalStatus status;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String achievabilityNote;

    @Setter
    @Column(name = "template_id")
    private Long templateId;

    @Setter
    @Column(name = "previous_version_id")
    private Long previousVersionId;

    @Setter
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Setter
    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Setter
    @Column(name = "approved_by")
    private Long approvedBy;

    @Setter
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = GoalStatus.DRAFT;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
