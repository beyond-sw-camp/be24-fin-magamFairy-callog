package org.example.backend.campaign.repository;

import org.example.backend.campaign.model.CampaignInvitation;
import org.example.backend.campaign.model.CampaignInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CampaignInvitationRepository extends JpaRepository<CampaignInvitation, Long> {
    boolean existsByCampaign_IdxAndInvitee_IdxAndStatus(
            Long campaignIdx,
            Long inviteeIdx,
            CampaignInvitationStatus status
    );

    boolean existsByCampaign_IdxAndInviteeOrganization_IdxAndStatus(
            Long campaignIdx,
            Long inviteeOrganizationIdx,
            CampaignInvitationStatus status
    );

    Optional<CampaignInvitation> findByIdxAndCampaign_Idx(Long idx, Long campaignIdx);
}
