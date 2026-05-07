package org.example.backend.kpi.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.backend.campaign.model.KpiCategory;
import org.example.backend.organization.model.Organization;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "kpi_template")
public class KpiTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Setter
    @Column(nullable = false, length = 200)
    private String name;

    @Setter
    @Column(name = "default_unit", length = 50)
    private String defaultUnit;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "default_category", length = 30)
    private KpiCategory defaultCategory;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "default_esg_category", length = 30)
    private EsgCategory defaultEsgCategory;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "default_kind", length = 30)
    private GoalKind defaultKind;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TemplateScope scope;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_org_id")
    private Organization ownerOrg;

    @Setter
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Setter
    @Builder.Default
    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.usageCount == null) {
            this.usageCount = 0;
        }
    }

    public void incrementUsageCount() {
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
    }
}
