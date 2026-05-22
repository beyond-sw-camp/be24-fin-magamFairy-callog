package org.example.backend.adcheck.repository;

import org.example.backend.adcheck.model.AdAiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdAiAnalysisRepository extends JpaRepository<AdAiAnalysis, Long> {
    Optional<AdAiAnalysis> findByAnalysisJobId(String analysisJobId);

    Optional<AdAiAnalysis> findByIdxAndCampaignIdx(Long idx, Long campaignIdx);

    List<AdAiAnalysis> findAllByCampaignIdxOrderByIdxDesc(Long campaignIdx);
}
