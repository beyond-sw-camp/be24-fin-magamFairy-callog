package org.example.backend.adcheck.repository;

import org.example.backend.adcheck.model.AdReviewRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AdReviewRequestRepository extends JpaRepository<AdReviewRequest, Long> {
    List<AdReviewRequest> findAllByOrderByIdxDesc();

    /** 대시보드 — 내 참여 캠페인의 특정 상태(예: REJECTED) 검수 요청. */
    List<AdReviewRequest> findAllByCampaignIdxInAndRequestStatus(Collection<Long> campaignIdx, String requestStatus);

    /** 대시보드 — 내 참여 캠페인의 모든 검수 요청 (검수목록/검수결과 분리는 서비스에서). */
    List<AdReviewRequest> findAllByCampaignIdxInOrderByIdxDesc(Collection<Long> campaignIdx);

    /** 대시보드(파트너=요청자 입장) — 내 참여 캠페인 중 우리 조직이 제출한 검수 요청 전체(상태 포함, 최신순). */
    List<AdReviewRequest> findAllByCampaignIdxInAndRequesterOrganizationIdxOrderByIdxDesc(
            Collection<Long> campaignIdx, Long requesterOrganizationIdx);

    List<AdReviewRequest> findAllByCampaignIdxOrderByIdxDesc(Long campaignIdx);

    List<AdReviewRequest> findAllByCampaignIdxAndRequesterOrganizationIdxOrderByIdxDesc(
            Long campaignIdx,
            Long requesterOrganizationIdx
    );

    List<AdReviewRequest> findAllByCampaignIdxAndRequesterLoginIdOrderByIdxDesc(
            Long campaignIdx,
            String requesterLoginId
    );

    boolean existsByCampaignIdxAndAdCheckJobId(Long campaignIdx, String adCheckJobId);
}
