package org.example.backend.campaign.repository;

import org.example.backend.campaign.model.CampaignApprovalFlow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampaignApprovalFlowRepository extends JpaRepository<CampaignApprovalFlow, Long> {
    List<CampaignApprovalFlow> findByCampaignIdxOrderBySequenceAsc(Long campaignIdx);

    Optional<CampaignApprovalFlow> findByIdxAndCampaignIdx(Long idx, Long campaignIdx);

    long countByCampaignIdx(Long campaignIdx);
}
