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

    /** 캠페인 ID 묶음으로 일괄 조회 (캘린더 일괄 fetch에서 사용). */
    java.util.List<CampaignIntro> findAllByCampaign_IdxIn(java.util.Collection<Long> campaignIdxes);

    @Modifying
    @Query("UPDATE CampaignIntro c SET c.viewCount = COALESCE(c.viewCount, 0) + 1 WHERE c.idx = :idx")
    int incrementViewCount(@Param("idx") Long idx);
}
