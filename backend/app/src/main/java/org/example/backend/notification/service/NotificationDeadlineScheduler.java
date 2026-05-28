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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
        Map<Long, List<CampaignMember>> membersByCampaign = loadMembersByCampaign(tasks);
        tasks.forEach(task -> notificationService.notifyDeadline(
                task,
                deadlineRecipients(task, membersByCampaign),
                type,
                keyPrefix + ":task:" + task.getIdx()
        ));
    }

    private Map<Long, List<CampaignMember>> loadMembersByCampaign(Collection<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Map.of();
        }

        Set<Long> campaignIdxs = tasks.stream()
                .map(this::resolveCampaignIdx)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (campaignIdxs.isEmpty()) {
            return Map.of();
        }

        return campaignMemberRepository.findAllByCampaignIdxIn(campaignIdxs).stream()
                .filter(member -> member.getCampaign() != null && member.getCampaign().getIdx() != null)
                .collect(Collectors.groupingBy(member -> member.getCampaign().getIdx()));
    }

    private List<User> deadlineRecipients(Task task, Map<Long, List<CampaignMember>> membersByCampaign) {
        Map<Long, User> recipients = new LinkedHashMap<>();

        if (task.getAssignee() != null && task.getAssignee().getIdx() != null) {
            recipients.put(task.getAssignee().getIdx(), task.getAssignee());
        }

        Long campaignIdx = resolveCampaignIdx(task);
        if (campaignIdx == null) {
            return List.copyOf(recipients.values());
        }

        membersByCampaign.getOrDefault(campaignIdx, List.of()).stream()
                .filter(member -> member.getCampaignRole() == CampaignMemberRole.MANAGER
                        || member.getCampaignRole() == CampaignMemberRole.GENERAL_MANAGER)
                .map(CampaignMember::getUser)
                .filter(user -> user != null && user.getIdx() != null)
                .forEach(user -> recipients.putIfAbsent(user.getIdx(), user));

        return List.copyOf(recipients.values());
    }

    private Long resolveCampaignIdx(Task task) {
        if (task == null) {
            return null;
        }
        if (task.getCampaign() != null) {
            return task.getCampaign().getIdx();
        }
        if (task.getTaskPart() != null && task.getTaskPart().getCampaign() != null) {
            return task.getTaskPart().getCampaign().getIdx();
        }
        if (task.getParticipant() != null && task.getParticipant().getCampaign() != null) {
            return task.getParticipant().getCampaign().getIdx();
        }
        return null;
    }
}
