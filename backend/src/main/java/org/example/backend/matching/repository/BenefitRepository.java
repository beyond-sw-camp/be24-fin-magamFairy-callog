package org.example.backend.matching.repository;

import org.example.backend.matching.model.PartnerBenefits;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BenefitRepository extends JpaRepository<PartnerBenefits,Long> {
}
