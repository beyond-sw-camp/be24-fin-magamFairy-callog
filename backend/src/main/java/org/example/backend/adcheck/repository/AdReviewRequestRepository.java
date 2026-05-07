package org.example.backend.adcheck.repository;

import org.example.backend.adcheck.model.AdReviewRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdReviewRequestRepository extends JpaRepository<AdReviewRequest, Long> {
    List<AdReviewRequest> findAllByOrderByIdxDesc();

    List<AdReviewRequest> findAllByCampaignIdxOrderByIdxDesc(Long campaignIdx);

    List<AdReviewRequest> findAllByCampaignIdxAndRequesterOrganizationIdxOrderByIdxDesc(
            Long campaignIdx,
            Long requesterOrganizationIdx
    );

    List<AdReviewRequest> findAllByCampaignIdxAndRequesterLoginIdOrderByIdxDesc(
            Long campaignIdx,
            String requesterLoginId
    );
}
