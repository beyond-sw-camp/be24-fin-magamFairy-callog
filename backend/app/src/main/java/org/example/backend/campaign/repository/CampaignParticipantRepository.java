package org.example.backend.campaign.repository;

import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignParticipant;
import org.example.backend.campaign.model.CampaignRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CampaignParticipantRepository extends JpaRepository<CampaignParticipant, Long> {
    boolean existsByCampaignIdxAndOrganizationIdx(Long campaignIdx, Long organizationIdx);

    boolean existsByCampaignIdxAndOrganizationIdxAndCampaignRole(
            Long campaignIdx, Long organizationIdx, CampaignRole campaignRole);

    List<CampaignParticipant> findAllByCampaignIdx(Long campaignIdx);

    @Query("SELECT DISTINCT cp.organization.idx FROM CampaignParticipant cp WHERE cp.campaign.idx = :campaignIdx")
    java.util.Set<Long> findOrganizationIdxByCampaignIdx(@Param("campaignIdx") Long campaignIdx);

    /**
     * 여러 캠페인의 참여자를 단일 IN 쿼리로 (N+1 회피).
     * Dashboard partnerProgress / summary 에서 캠페인 N개 × 1쿼리 → 1쿼리.
     * organization 까지 fetch join 으로 LAZY 해결.
     */
    @Query("SELECT cp FROM CampaignParticipant cp " +
           "JOIN FETCH cp.organization " +
           "WHERE cp.campaign.idx IN :campaignIds")
    List<CampaignParticipant> findAllByCampaignIdxInWithOrg(@Param("campaignIds") java.util.Collection<Long> campaignIds);

    java.util.Optional<CampaignParticipant> findFirstByCampaignIdxAndCampaignRole(
            Long campaignIdx, CampaignRole campaignRole);

    /**
     * 조직 기준으로 참여 캠페인 조회 — HQ 대시보드 "조직 전체" 토글용.
     * Campaign 자체는 단일 SELECT, 그 안의 partners/tags 는 Campaign 엔티티에 부여된
     * @BatchSize(50) 으로 배치 로딩되어 N+1 회피.
     */
    @Query("SELECT DISTINCT cp.campaign FROM CampaignParticipant cp " +
           "WHERE cp.organization.idx = :orgIdx " +
           "ORDER BY cp.campaign.idx DESC")
    List<Campaign> findCampaignsByOrganizationIdx(@Param("orgIdx") Long orgIdx);
}
