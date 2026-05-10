package org.example.backend.campaign.repository;

import org.example.backend.campaign.model.CampaignIntro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampaignIntroRepository extends JpaRepository<CampaignIntro, Long> {
    Optional<CampaignIntro> findByCampaign_Idx(Long campaignIdx);

    @Modifying
    @Query("UPDATE CampaignIntro c SET c.viewCount = COALESCE(c.viewCount, 0) + 1 WHERE c.idx = :idx")
    int incrementViewCount(@Param("idx") Long idx);
}
