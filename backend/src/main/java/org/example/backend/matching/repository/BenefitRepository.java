package org.example.backend.matching.repository;

import org.example.backend.matching.model.PartnerBenefits;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BenefitRepository extends JpaRepository<PartnerBenefits,Long> {
    List<PartnerBenefits> findAllByCampaignIdx(Long campaignIdx);
}
