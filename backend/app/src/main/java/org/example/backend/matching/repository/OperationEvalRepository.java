package org.example.backend.matching.repository;

import org.example.backend.matching.model.evaluation.OperationEval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperationEvalRepository extends JpaRepository<OperationEval, Integer> {
}
