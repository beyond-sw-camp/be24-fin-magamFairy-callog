package org.example.backend.matching.repository;

import org.example.backend.matching.model.evaluation.BrandEval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandEvalRepository extends JpaRepository<BrandEval, Long> {
}
