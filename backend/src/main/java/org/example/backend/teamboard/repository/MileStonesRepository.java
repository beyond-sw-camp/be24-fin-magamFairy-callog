package org.example.backend.teamboard.repository;

import org.example.backend.teamboard.model.MileStones;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MileStonesRepository extends JpaRepository<MileStones, Long> {
    List<MileStones> findAllByCampaign_IdxOrderBySortOrderAscIdxAsc(Long campaignIdx);
}
