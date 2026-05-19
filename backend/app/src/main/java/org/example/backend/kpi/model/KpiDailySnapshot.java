package org.example.backend.kpi.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.backend.organization.model.Organization;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Organization 단위 일별 평균 KPI 달성률 스냅샷.
 * 매일 새벽 SnapshotScheduler 가 organization별 KPI 평균을 계산해 row 추가.
 * 제휴사 sparkline (recent7d) 의 원천 데이터.
 * (organization, snapshot_date) 유니크.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "kpi_daily_snapshot",
        uniqueConstraints = @UniqueConstraint(name = "uk_kpi_daily_org_date",
                columnNames = {"organization_id", "snapshot_date"}),
        indexes = @Index(name = "idx_kpi_daily_org_date", columnList = "organization_id,snapshot_date"))
public class KpiDailySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate date;

    @Column(name = "avg_kpi_percent")
    private Integer avgKpiPercent;

    @CreationTimestamp
    @Column(name = "snapshot_at", updatable = false)
    private LocalDateTime snapshotAt;
}
