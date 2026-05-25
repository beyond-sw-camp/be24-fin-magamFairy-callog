package org.example.notification.deadline;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// app 도메인 엔티티를 import하지 않고 JDBC로 마감 알림 후보만 읽어오는 어댑터입니다.
@Repository
@RequiredArgsConstructor
public class JdbcDeadlineNotificationCandidateRepository implements DeadlineNotificationCandidateRepository {
    private static final List<String> DONE_STATUSES = List.of("DONE");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<DeadlineTaskCandidate> findTasksDueBetween(LocalDateTime start, LocalDateTime end) {
        return jdbcTemplate.query(
                """
                SELECT t.idx AS task_id,
                       t.name AS task_name,
                       t.due_date AS due_date,
                       tp.campaign_id AS campaign_id
                FROM Task t
                LEFT JOIN TaskParts tp ON tp.idx = t.task_part_id
                WHERE t.due_date BETWEEN :start AND :end
                  AND t.status NOT IN (:doneStatuses)
                """,
                new MapSqlParameterSource()
                        .addValue("start", Timestamp.valueOf(start))
                        .addValue("end", Timestamp.valueOf(end))
                        .addValue("doneStatuses", DONE_STATUSES),
                (rs, rowNum) -> new DeadlineTaskCandidate(
                        rs.getLong("task_id"),
                        rs.getString("task_name"),
                        rs.getTimestamp("due_date").toLocalDateTime(),
                        rs.getObject("campaign_id", Long.class)
                )
        );
    }

    @Override
    public List<DeadlineTaskCandidate> findOverdueTasks(LocalDateTime now) {
        return jdbcTemplate.query(
                """
                SELECT t.idx AS task_id,
                       t.name AS task_name,
                       t.due_date AS due_date,
                       tp.campaign_id AS campaign_id
                FROM Task t
                LEFT JOIN TaskParts tp ON tp.idx = t.task_part_id
                WHERE t.due_date < :now
                  AND t.status NOT IN (:doneStatuses)
                """,
                new MapSqlParameterSource()
                        .addValue("now", Timestamp.valueOf(now))
                        .addValue("doneStatuses", DONE_STATUSES),
                (rs, rowNum) -> new DeadlineTaskCandidate(
                        rs.getLong("task_id"),
                        rs.getString("task_name"),
                        rs.getTimestamp("due_date").toLocalDateTime(),
                        rs.getObject("campaign_id", Long.class)
                )
        );
    }

    @Override
    public List<DeadlineRecipient> findRecipients(DeadlineTaskCandidate task) {
        Map<Long, DeadlineRecipient> recipients = new LinkedHashMap<>();
        findAssignee(task.taskId()).forEach(recipient -> recipients.put(recipient.userId(), recipient));

        if (task.campaignId() != null) {
            findCampaignManagers(task.campaignId())
                    .forEach(recipient -> recipients.putIfAbsent(recipient.userId(), recipient));
        }

        return List.copyOf(recipients.values());
    }

    private List<DeadlineRecipient> findAssignee(Long taskId) {
        return jdbcTemplate.query(
                """
                SELECT u.idx AS user_id,
                       u.organization_id AS organization_id,
                       u.role AS role
                FROM Task t
                JOIN `user` u ON u.idx = t.assignee_id
                WHERE t.idx = :taskId
                """,
                new MapSqlParameterSource("taskId", taskId),
                (rs, rowNum) -> new DeadlineRecipient(
                        rs.getLong("user_id"),
                        rs.getObject("organization_id", Long.class),
                        rs.getString("role")
                )
        );
    }

    private List<DeadlineRecipient> findCampaignManagers(Long campaignId) {
        return jdbcTemplate.query(
                """
                SELECT u.idx AS user_id,
                       u.organization_id AS organization_id,
                       u.role AS role
                FROM campaign_members cm
                JOIN `user` u ON u.idx = cm.user_idx
                WHERE cm.campaign_idx = :campaignId
                  AND cm.campaign_role IN (:roles)
                """,
                new MapSqlParameterSource()
                        .addValue("campaignId", campaignId)
                        .addValue("roles", List.of("MANAGER", "GENERAL_MANAGER")),
                (rs, rowNum) -> new DeadlineRecipient(
                        rs.getLong("user_id"),
                        rs.getObject("organization_id", Long.class),
                        rs.getString("role")
                )
        );
    }
}
