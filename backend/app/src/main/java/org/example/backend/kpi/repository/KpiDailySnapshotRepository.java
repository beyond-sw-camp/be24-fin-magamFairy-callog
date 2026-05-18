package org.example.backend.kpi.repository;

import org.example.backend.kpi.model.KpiDailySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface KpiDailySnapshotRepository extends JpaRepository<KpiDailySnapshot, Long> {

    List<KpiDailySnapshot> findAllByOrganization_IdxAndDateGreaterThanEqualOrderByDateAsc(
            Long organizationId, LocalDate from);

    Optional<KpiDailySnapshot> findByOrganization_IdxAndDate(Long organizationId, LocalDate date);
}
