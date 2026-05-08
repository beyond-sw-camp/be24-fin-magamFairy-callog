package org.example.backend.matching.repository;

import org.example.backend.matching.model.PartnerBenefits;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BenefitRepository extends JpaRepository<PartnerBenefits,Long> {

    /** 특정 organization이 제출한 PartnerBenefits 의 캠페인 idx 목록 (= 제안서 제출 이력). */
    @Query("SELECT DISTINCT b.campaign.idx FROM PartnerBenefits b " +
            "WHERE b.organization.idx = :orgIdx AND b.campaign IS NOT NULL")
    List<Long> findCampaignIdxByOrganizationIdx(@Param("orgIdx") Long orgIdx);
}
