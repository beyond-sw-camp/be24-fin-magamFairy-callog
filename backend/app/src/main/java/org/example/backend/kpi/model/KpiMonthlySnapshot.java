package org.example.backend.kpi.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OrganizationKpi 의 월별 actual/target 스냅샷.
 * 매월 1일 새벽 SnapshotScheduler 가 누적 데이터를 기반으로 row 추가.
 * (orgKpi, year, month) 유니크.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "kpi_monthly_snapshot",
        uniqueConstraints = @UniqueConstraint(name = "uk_kpi_monthly_orgkpi_ym",
                columnNames = {"org_kpi_id", "snapshot_year", "snapshot_month"}),
        indexes = @Index(name = "idx_kpi_monthly_orgkpi", columnList = "org_kpi_id"))
public class KpiMonthlySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_kpi_id", nullable = false)
    private OrganizationKpi orgKpi;

    @Column(name = "snapshot_year", nullable = false)
    private int year;

    @Column(name = "snapshot_month", nullable = false)
    private int month;

    @Column(precision = 18, scale = 4)
    private BigDecimal actualValue;

    @Column(precision = 18, scale = 4)
    private BigDecimal targetValue;

    @CreationTimestamp
    @Column(name = "snapshot_at", updatable = false)
    private LocalDateTime snapshotAt;
}
