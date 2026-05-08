package org.example.backend.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.adcheck.model.AdReviewRequest;
import org.example.backend.notification.model.Notification;
import org.example.backend.notification.model.NotificationDto;
import org.example.backend.notification.model.NotificationSeverity;
import org.example.backend.notification.model.NotificationType;
import org.example.backend.notification.repository.NotificationRepository;
import org.example.backend.teamboard.model.Task;
import org.example.backend.teamboard.model.TaskStatus;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private static final int DEFAULT_LIST_LIMIT = 50;
    private static final int MAX_LIST_LIMIT = 100;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationSseService notificationSseService;

    public NotificationDto.ListRes list(Long recipientIdx, Integer count) {
        int limit = count == null ? DEFAULT_LIST_LIMIT : Math.min(Math.max(count, 1), MAX_LIST_LIMIT);
        List<NotificationDto.Res> notifications = notificationRepository
                .findAllByRecipient_IdxOrderByCreatedAtDesc(recipientIdx, PageRequest.of(0, limit))
                .stream()
                .map(NotificationDto.Res::from)
                .toList();
        long unreadCount = notificationRepository.countByRecipient_IdxAndIsReadFalse(recipientIdx);

        return new NotificationDto.ListRes(notifications, unreadCount);
    }

    @Transactional
    public NotificationDto.Res confirm(Long recipientIdx, Long notificationIdx) {
        Notification notification = notificationRepository.findByIdxAndRecipient_Idx(notificationIdx, recipientIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "notification not found."));

        notification.markAsRead();
        return NotificationDto.Res.from(notification);
    }

    @Transactional
    public NotificationDto.ListRes confirmAll(Long recipientIdx) {
        notificationRepository.findAllByRecipient_IdxAndIsReadFalse(recipientIdx)
                .forEach(Notification::markAsRead);

        return list(recipientIdx, DEFAULT_LIST_LIMIT);
    }

    @Transactional
    public NotificationDto.Res create(NotificationDto.CreateReq request, User sender) {
        if (request == null || request.recipientIdx() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recipientIdx is required.");
        }

        User recipient = userRepository.findById(request.recipientIdx())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "recipient not found."));

        return create(
                recipient,
                sender,
                request.type(),
                request.severity(),
                request.title(),
                request.message(),
                request.detail(),
                request.targetLabel(),
                request.targetUrl()
        );
    }

    @Transactional
    public NotificationDto.Res create(
            User recipient,
            User sender,
            NotificationType type,
            NotificationSeverity severity,
            String title,
            String message,
            String detail,
            String targetLabel,
            String targetUrl
    ) {
        if (recipient == null || recipient.getIdx() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recipient is required.");
        }

        Notification notification = notificationRepository.save(Notification.builder()
                .recipient(recipient)
                .sender(sender)
                .type(type == null ? NotificationType.SYSTEM : type)
                .severity(severity == null ? NotificationSeverity.NORMAL : severity)
                .title(nonBlank(title, "Notification"))
                .message(nonBlank(message, "New notification has arrived."))
                .detail(detail)
                .targetLabel(targetLabel)
                .targetUrl(targetUrl)
                .build());
        NotificationDto.Res response = NotificationDto.Res.from(notification);
        notificationSseService.sendToUser(recipient.getIdx(), response);

        return response;
    }

    @Transactional
    public void createForRecipients(
            Collection<User> recipients,
            User sender,
            NotificationType type,
            NotificationSeverity severity,
            String title,
            String message,
            String detail,
            String targetLabel,
            String targetUrl
    ) {
        uniqueUsers(recipients).forEach(recipient ->
                create(recipient, sender, type, severity, title, message, detail, targetLabel, targetUrl));
    }

    @Transactional
    public void notifyTaskAssigned(Task task, User sender) {
        if (task == null || task.getAssignee() == null) {
            return;
        }

        create(
                task.getAssignee(),
                sender,
                NotificationType.TASK_ASSIGNED,
                NotificationSeverity.NORMAL,
                "새 업무가 배정되었습니다.",
                task.getName(),
                "배정된 업무의 담당자, 마감일, 우선순위를 확인해 주세요.",
                "업무 보드로 이동",
                "/team-board"
        );
    }

    @Transactional
    public void notifyTaskStatusChanged(Task task, TaskStatus previousStatus, TaskStatus nextStatus, User sender) {
        if (task == null || task.getAssignee() == null || previousStatus == nextStatus) {
            return;
        }

        create(
                task.getAssignee(),
                sender,
                NotificationType.TASK_STATUS_CHANGED,
                NotificationSeverity.NORMAL,
                "업무 상태가 변경되었습니다.",
                task.getName() + " : " + previousStatus + " -> " + nextStatus,
                "담당 업무의 진행 상태가 변경되었습니다.",
                "업무 보드로 이동",
                "/team-board"
        );
    }

    @Transactional
    public void notifyReviewRequested(AdReviewRequest request, User requester, Collection<User> reviewers) {
        if (request == null || request.getCampaign() == null) {
            return;
        }

        createForRecipients(
                reviewers,
                requester,
                NotificationType.REVIEW_REQUESTED,
                NotificationSeverity.HIGH,
                "검수 요청이 도착했습니다.",
                request.getFileName(),
                "새로운 검수 요청이 생성되었습니다. 요청 자료와 메모를 확인해 주세요.",
                "검수 요청 보기",
                "/campaigns/" + request.getCampaign().getIdx()
        );
    }

    @Transactional
    public void notifyReviewDecision(AdReviewRequest request, User reviewer, User requester, boolean approved) {
        if (request == null || requester == null || request.getCampaign() == null) {
            return;
        }

        create(
                requester,
                reviewer,
                approved ? NotificationType.REVIEW_APPROVED : NotificationType.REVIEW_REJECTED,
                approved ? NotificationSeverity.NORMAL : NotificationSeverity.HIGH,
                approved ? "검수 요청이 승인되었습니다." : "검수 요청이 반려되었습니다.",
                request.getFileName(),
                approved ? "검수 요청이 승인되었습니다." : "검수 요청이 반려되었습니다. 반려 사유를 확인해 주세요.",
                "검수 결과 보기",
                "/campaigns/" + request.getCampaign().getIdx()
        );
    }

    private List<User> uniqueUsers(Collection<User> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }

        Map<Long, User> unique = new LinkedHashMap<>();
        users.stream()
                .filter(user -> user != null && user.getIdx() != null)
                .forEach(user -> unique.putIfAbsent(user.getIdx(), user));
        return List.copyOf(unique.values());
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
