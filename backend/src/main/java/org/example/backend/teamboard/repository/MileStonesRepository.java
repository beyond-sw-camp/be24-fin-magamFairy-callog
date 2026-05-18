package org.example.backend.teamboard.repository;

import org.example.backend.teamboard.model.MileStones;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MileStonesRepository extends JpaRepository<MileStones, Long> {
    List<MileStones> findAllByCampaign_IdxOrderBySortOrderAscIdxAsc(Long campaignIdx);

    /** 캠페인 ID 묶음으로 일괄 조회 (캘린더 일괄 fetch에서 사용). */
    List<MileStones> findAllByCampaign_IdxIn(java.util.Collection<Long> campaignIdxes);
}
