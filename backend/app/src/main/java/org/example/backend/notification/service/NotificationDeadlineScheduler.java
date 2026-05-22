package org.example.backend.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.CampaignMember;
import org.example.backend.campaign.model.CampaignMemberRole;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.common.redis.RedisLock;
import org.example.backend.notification.model.NotificationType;
import org.example.backend.teamboard.model.Task;
import org.example.backend.teamboard.model.TaskStatus;
import org.example.backend.teamboard.repository.TaskRepository;
import org.example.backend.user.model.User;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationDeadlineScheduler {
    private static final List<TaskStatus> DONE_STATUSES = List.of(TaskStatus.DONE);

    private final TaskRepository taskRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final NotificationService notificationService;
    private final RedisLock redisLock;

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void publishDeadlineNotifications() {
        String token = redisLock.tryLock("lock:notification:deadline", Duration.ofMinutes(4));
        if (token == null) return;   // 다른 Pod이 이미 실행 중 → skip
        try {
            LocalDateTime now = LocalDateTime.now();

            notifyWindow(
                    taskRepository.findAllByDueDateBetweenAndStatusNotIn(
                            now.plusHours(24).minusMinutes(3),
                            now.plusHours(24).plusMinutes(3),
                            DONE_STATUSES
                    ),
                    NotificationType.DEADLINE_24H,
                    "deadline:24h"
            );
            notifyWindow(
                    taskRepository.findAllByDueDateBetweenAndStatusNotIn(
                            now.plusHours(1).minusMinutes(3),
                            now.plusHours(1).plusMinutes(3),
                            DONE_STATUSES
                    ),
                    NotificationType.DEADLINE_1H,
                    "deadline:1h"
            );
            notifyWindow(
                    taskRepository.findAllByDueDateBeforeAndStatusNotIn(now, DONE_STATUSES),
                    NotificationType.DEADLINE_OVERDUE,
                    "deadline:overdue"
            );
        } finally {
            redisLock.unLock("lock:notification:deadline", token);
        }
    }

    private void notifyWindow(Collection<Task> tasks, NotificationType type, String keyPrefix) {
        tasks.forEach(task -> notificationService.notifyDeadline(
                task,
                deadlineRecipients(task),
                type,
                keyPrefix + ":task:" + task.getIdx()
        ));
    }

    private List<User> deadlineRecipients(Task task) {
        Map<Long, User> recipients = new LinkedHashMap<>();

        if (task.getAssignee() != null && task.getAssignee().getIdx() != null) {
            recipients.put(task.getAssignee().getIdx(), task.getAssignee());
        }

        Long campaignIdx = task.getTaskPart() != null && task.getTaskPart().getCampaign() != null
                ? task.getTaskPart().getCampaign().getIdx()
                : null;
        if (campaignIdx == null) {
            return List.copyOf(recipients.values());
        }

        campaignMemberRepository.findAllByCampaignIdx(campaignIdx).stream()
                .filter(member -> member.getCampaignRole() == CampaignMemberRole.MANAGER
                        || member.getCampaignRole() == CampaignMemberRole.GENERAL_MANAGER)
                .map(CampaignMember::getUser)
                .filter(user -> user != null && user.getIdx() != null)
                .forEach(user -> recipients.putIfAbsent(user.getIdx(), user));

        return List.copyOf(recipients.values());
    }
}
