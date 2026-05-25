package org.example.notification.deadline;

import org.example.notification.event.NotificationEvent;
import org.example.notification.event.NotificationEventType;
import org.example.notification.event.NotificationReferenceType;
import org.example.notification.event.NotificationSeverity;
import org.example.notification.service.NotificationCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

// 마감 임박/초과 업무를 조회해서 NotificationEvent로 변환하는 서비스입니다.
@Service
public class DeadlineNotificationService {
    private static final String TARGET_LABEL = "업무 보드로 이동";
    private static final String TARGET_URL = "/team-board";

    private final DeadlineNotificationCandidateRepository candidateRepository;
    private final NotificationCommandService commandService;
    private final Clock clock;

    public DeadlineNotificationService(
            DeadlineNotificationCandidateRepository candidateRepository,
            NotificationCommandService commandService
    ) {
        this(candidateRepository, commandService, Clock.systemDefaultZone());
    }

    DeadlineNotificationService(
            DeadlineNotificationCandidateRepository candidateRepository,
            NotificationCommandService commandService,
            Clock clock
    ) {
        this.candidateRepository = candidateRepository;
        this.commandService = commandService;
        this.clock = clock;
    }

    // 24시간 전, 1시간 전, 마감 초과 업무를 찾아 알림 이벤트로 생성합니다.
    @Transactional
    public void publishDeadlineNotifications() {
        LocalDateTime now = LocalDateTime.now(clock);

        notifyWindow(
                candidateRepository.findTasksDueBetween(
                        now.plusHours(24).minusMinutes(3),
                        now.plusHours(24).plusMinutes(3)
                ),
                NotificationEventType.DEADLINE_24H,
                "deadline:24h"
        );
        notifyWindow(
                candidateRepository.findTasksDueBetween(
                        now.plusHours(1).minusMinutes(3),
                        now.plusHours(1).plusMinutes(3)
                ),
                NotificationEventType.DEADLINE_1H,
                "deadline:1h"
        );
        notifyWindow(
                candidateRepository.findOverdueTasks(now),
                NotificationEventType.DEADLINE_OVERDUE,
                "deadline:overdue"
        );
    }

    // 한 마감 구간에 속한 업무별로 중복 수신자를 제거한 뒤 알림을 생성합니다.
    private void notifyWindow(
            Collection<DeadlineTaskCandidate> tasks,
            NotificationEventType eventType,
            String eventIdPrefix
    ) {
        tasks.forEach(task -> distinctRecipients(task).values()
                .forEach(recipient -> commandService.createFromEvent(toEvent(task, recipient, eventType, eventIdPrefix))));
    }

    private Map<Long, DeadlineRecipient> distinctRecipients(DeadlineTaskCandidate task) {
        Map<Long, DeadlineRecipient> recipients = new LinkedHashMap<>();
        candidateRepository.findRecipients(task).stream()
                .filter(recipient -> recipient != null && recipient.userId() != null)
                .forEach(recipient -> recipients.putIfAbsent(recipient.userId(), recipient));
        return recipients;
    }

    private NotificationEvent toEvent(
            DeadlineTaskCandidate task,
            DeadlineRecipient recipient,
            NotificationEventType eventType,
            String eventIdPrefix
    ) {
        return new NotificationEvent(
                eventIdPrefix + ":task:" + task.taskId() + ":user:" + recipient.userId(),
                eventType,
                recipient.userId(),
                recipient.organizationId(),
                recipient.role(),
                null,
                null,
                title(eventType),
                task.taskName(),
                "업무 마감일과 현재 진행 상태를 확인해 주세요.",
                TARGET_LABEL,
                TARGET_URL,
                NotificationReferenceType.DEADLINE,
                task.taskId(),
                eventType.name(),
                severity(eventType),
                clock.instant().toString(),
                "1"
        );
    }

    private String title(NotificationEventType eventType) {
        return switch (eventType) {
            case DEADLINE_24H -> "업무 마감이 24시간 남았습니다.";
            case DEADLINE_1H -> "업무 마감이 1시간 남았습니다.";
            case DEADLINE_OVERDUE -> "업무 마감이 초과되었습니다.";
            default -> "업무 마감 알림";
        };
    }

    private NotificationSeverity severity(NotificationEventType eventType) {
        return eventType == NotificationEventType.DEADLINE_OVERDUE
                ? NotificationSeverity.CRITICAL
                : NotificationSeverity.HIGH;
    }
}
