package org.example.backend.campaign.repository;

import org.example.backend.campaign.model.CampaignMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CampaignMemberRepository extends JpaRepository<CampaignMember, Long> {

    @EntityGraph(attributePaths = {"user", "user.organization"})
    List<CampaignMember> findAllByCampaignIdx(Long campaignIdx);

    Optional<CampaignMember> findByCampaignIdxAndUserIdx(Long campaignIdx, Long userIdx);

    @Query("SELECT cm FROM CampaignMember cm JOIN FETCH cm.campaign c " +
           "WHERE cm.user.idx = :userIdx ORDER BY c.idx DESC")
    List<CampaignMember> findAllWithCampaignByUserIdx(@Param("userIdx") Long userIdx);

    boolean existsByCampaignIdxAndUserIdx(Long campaignIdx, Long userIdx);
}
