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

    /**
     * 개인 업무: 캠페인 연결(직접 campaign/참여사/마일스톤/업무파트)이 전혀 없고 담당자가 본인인 Task.
     */
    List<Task> findAllByAssignee_IdxAndCampaignIsNullAndParticipantIsNullAndMilestoneIsNullAndTaskPartIsNullOrderByIdxDesc(Long assigneeIdx);

    List<Task> findAllByTaskPart_Campaign_IdxInOrderByIdxDesc(Collection<Long> campaignIds);

    /** 1급화: 캠페인 직접 연결(campaign_id) · 업무파트 경유 · 참여사 경유 모두 커버 (단일 캠페인).
     *  LEFT JOIN 으로 null 경로가 행을 배제하지 않게 함. */
    @org.springframework.data.jpa.repository.Query(
            "SELECT t FROM Task t "
                    + "LEFT JOIN t.campaign c LEFT JOIN t.taskPart tp LEFT JOIN tp.campaign tpc "
                    + "LEFT JOIN t.participant p LEFT JOIN p.campaign pc "
                    + "WHERE c.idx = :cid OR tpc.idx = :cid OR pc.idx = :cid ORDER BY t.idx DESC")
    List<Task> findAllByCampaignDirectOrViaTaskPart(
            @org.springframework.data.repository.query.Param("cid") Long campaignIdx);

    /** 1급화: 여러 캠페인 — 직접 · 업무파트 · 참여사 경유 (LEFT JOIN). */
    @org.springframework.data.jpa.repository.Query(
            "SELECT t FROM Task t "
                    + "LEFT JOIN t.campaign c LEFT JOIN t.taskPart tp LEFT JOIN tp.campaign tpc "
                    + "LEFT JOIN t.participant p LEFT JOIN p.campaign pc "
                    + "WHERE c.idx IN :cids OR tpc.idx IN :cids OR pc.idx IN :cids ORDER BY t.idx DESC")
    List<Task> findAllByCampaignIdsDirectOrViaTaskPart(
            @org.springframework.data.repository.query.Param("cids") Collection<Long> campaignIds);

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
