package org.example.backend.teamboard.repository;

import org.example.backend.teamboard.model.Task;
import org.example.backend.teamboard.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByTaskPart_Campaign_IdxOrderByIdxDesc(Long campaignIdx);

    List<Task> findAllByOrderByIdxDesc();

    List<Task> findAllByAssignee_IdxOrderByIdxDesc(Long assigneeIdx);

    List<Task> findAllByTaskPart_Campaign_IdxInOrderByIdxDesc(Collection<Long> campaignIds);

    List<Task> findAllByDueDateBetweenAndStatusNotIn(
            LocalDateTime start,
            LocalDateTime end,
            Collection<TaskStatus> statuses
    );

    List<Task> findAllByDueDateBeforeAndStatusNotIn(LocalDateTime dueDate, Collection<TaskStatus> statuses);
}
