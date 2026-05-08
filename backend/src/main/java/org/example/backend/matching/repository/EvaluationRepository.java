package org.example.backend.matching.repository;

import org.example.backend.matching.model.evaluation.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    Optional<Evaluation> findBySessionId(String attr0);


    @Query("select e from Evaluation e " +
            "join fetch e.benefits " +
            "join fetch e.campaign " +
            "join fetch e.customer " +
            "join fetch e.revenue " +
            "join fetch e.brand " +
            "join fetch e.operation " +
            "join fetch e.cost " +
            "where e.campaign.idx = :campaignIdx")
    List<Evaluation> findAllByCampaignIdx(@Param("campaignIdx") Long campaignIdx);
//    Optional<Evaluation> findByCampaignIdxAndOrganizationIdx(Long campaignIdx, Long organizationIdx);
}
