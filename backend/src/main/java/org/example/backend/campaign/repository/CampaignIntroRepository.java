package org.example.backend.campaign.repository;

import org.example.backend.campaign.model.CampaignIntro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampaignIntroRepository extends JpaRepository<CampaignIntro, Long> {
    Optional<CampaignIntro> findByCampaign_Idx(Long campaignIdx);
}
