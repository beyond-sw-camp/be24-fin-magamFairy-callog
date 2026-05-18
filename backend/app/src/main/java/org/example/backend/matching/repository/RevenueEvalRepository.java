package org.example.backend.matching.repository;

import org.example.backend.matching.model.evaluation.RevenueEval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RevenueEvalRepository extends JpaRepository<RevenueEval, Long> {
}
