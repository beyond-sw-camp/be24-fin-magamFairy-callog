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

    /** 개인 업무: 캠페인 연결(참여사/마일스톤/업무파트)이 전혀 없고 담당자가 본인인 Task. */
    List<Task> findAllByAssignee_IdxAndParticipantIsNullAndMilestoneIsNullAndTaskPartIsNullOrderByIdxDesc(Long assigneeIdx);

    List<Task> findAllByTaskPart_Campaign_IdxInOrderByIdxDesc(Collection<Long> campaignIds);

    List<Task> findAllByDueDateBetweenAndStatusNotIn(
            LocalDateTime start,
            LocalDateTime end,
            Collection<TaskStatus> statuses
    );

    List<Task> findAllByDueDateBeforeAndStatusNotIn(LocalDateTime dueDate, Collection<TaskStatus> statuses);

    /**
     * 한 번의 쿼리로 여러 캠페인의 task 수를 GROUP BY 로 집계.
     * 결과: [campaignIdx, count] 쌍 리스트. task가 없는 캠페인은 결과에 포함되지 않음.
     * N+1 회피용 — DashboardView / CampaignList totalTaskCount 채울 때 사용.
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT t.taskPart.campaign.idx, COUNT(t) " +
            "FROM Task t " +
            "WHERE t.taskPart.campaign.idx IN :campaignIds " +
            "GROUP BY t.taskPart.campaign.idx"
    )
    List<Object[]> countByCampaignIdxIn(
            @org.springframework.data.repository.query.Param("campaignIds") Collection<Long> campaignIds
    );
}
