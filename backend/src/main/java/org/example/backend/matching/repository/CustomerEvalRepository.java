package org.example.backend.matching.repository;

import org.example.backend.matching.model.evaluation.CustomerEval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerEvalRepository extends JpaRepository<CustomerEval, Long> {
}
