package org.example.backend.teamboard.repository;

import org.example.backend.teamboard.model.TaskParts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskPartsRepository extends JpaRepository<TaskParts, Long> {
    List<TaskParts> findAllByCampaign_IdxOrderBySortOrderAscIdxAsc(Long campaignIdx);

    List<TaskParts> findAllByMileStones_IdxOrderBySortOrderAscIdxAsc(Long milestoneIdx);
}
