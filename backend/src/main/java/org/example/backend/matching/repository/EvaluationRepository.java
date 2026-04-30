package org.example.backend.matching.repository;

import org.example.backend.matching.model.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    Optional<Evaluation> findByCampaignIdxAndOrganizationIdx(Long campaignIdx, Long organizationIdx);
}
