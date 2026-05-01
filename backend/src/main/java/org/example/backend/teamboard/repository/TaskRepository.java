package org.example.backend.teamboard.repository;

import org.example.backend.teamboard.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByTaskPart_Campaign_IdxOrderByIdxDesc(Long campaignIdx);

    List<Task> findAllByOrderByIdxDesc();

    List<Task> findAllByAssignee_IdxOrderByIdxDesc(Long assigneeIdx);
}
