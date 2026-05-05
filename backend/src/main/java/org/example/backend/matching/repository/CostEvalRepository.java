package org.example.backend.matching.repository;

import org.example.backend.matching.model.evaluation.CostEval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CostEvalRepository extends JpaRepository<CostEval, Integer> {
}
