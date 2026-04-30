package org.example.backend.campaign.repository;

import org.example.backend.campaign.model.CampaignParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignParticipantRepository extends JpaRepository<CampaignParticipant, Long> {
    // 캠페인 ID와 단체(Organization)의 IDX를 조건으로 존재 여부 확인
    boolean existsByCampaignIdxAndOrganizationIdx(Long campaignIdx, Long organizationIdx);
}
