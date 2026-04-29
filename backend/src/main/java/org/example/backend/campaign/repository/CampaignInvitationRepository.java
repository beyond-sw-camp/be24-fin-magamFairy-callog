package org.example.backend.campaign.repository;

import org.example.backend.campaign.model.CampaignInvitation;
import org.example.backend.campaign.model.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampaignInvitationRepository extends JpaRepository<CampaignInvitation, Long> {
    List<CampaignInvitation> findByCampaignIdxOrderByIdxDesc(Long campaignIdx);

    List<CampaignInvitation> findByCampaignIdxAndStatus(Long campaignIdx, InvitationStatus status);

    Optional<CampaignInvitation> findByIdxAndCampaignIdx(Long idx, Long campaignIdx);

    Optional<CampaignInvitation> findByInvitationToken(String invitationToken);
}
