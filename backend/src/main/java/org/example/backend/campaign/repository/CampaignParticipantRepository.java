package org.example.backend.campaign.repository;

import org.example.backend.campaign.model.CampaignParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampaignParticipantRepository extends JpaRepository<CampaignParticipant, Long> {
    List<CampaignParticipant> findByCampaignIdxOrderByIdxDesc(Long campaignIdx);

    Optional<CampaignParticipant> findByIdxAndCampaignIdx(Long idx, Long campaignIdx);

    Optional<CampaignParticipant> findByCampaignIdxAndUserLoginId(Long campaignIdx, String userLoginId);
}
