package org.example.backend.campaign.repository;

import org.example.backend.campaign.model.CampaignParticipant;
import org.example.backend.campaign.model.CampaignRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignParticipantRepository extends JpaRepository<CampaignParticipant, Long> {
    boolean existsByCampaignIdxAndOrganizationIdx(Long campaignIdx, Long organizationIdx);

    boolean existsByCampaignIdxAndOrganizationIdxAndCampaignRole(
            Long campaignIdx, Long organizationIdx, CampaignRole campaignRole);

    List<CampaignParticipant> findAllByCampaignIdx(Long campaignIdx);
}
