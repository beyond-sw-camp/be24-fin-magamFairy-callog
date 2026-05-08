package org.example.backend.kpi.repository;

import org.example.backend.kpi.model.KpiMonthlySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KpiMonthlySnapshotRepository extends JpaRepository<KpiMonthlySnapshot, Long> {

    List<KpiMonthlySnapshot> findAllByOrgKpi_IdxAndYearOrderByMonthAsc(Long orgKpiId, int year);

    Optional<KpiMonthlySnapshot> findByOrgKpi_IdxAndYearAndMonth(Long orgKpiId, int year, int month);
}
