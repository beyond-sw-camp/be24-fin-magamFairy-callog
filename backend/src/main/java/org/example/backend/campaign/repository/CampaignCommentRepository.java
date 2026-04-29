package org.example.backend.campaign.repository;

import org.example.backend.campaign.model.CampaignComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampaignCommentRepository extends JpaRepository<CampaignComment, Long> {
    List<CampaignComment> findByCampaignIdxOrderByIdxDesc(Long campaignIdx);

    Optional<CampaignComment> findByIdxAndCampaignIdx(Long idx, Long campaignIdx);
}
