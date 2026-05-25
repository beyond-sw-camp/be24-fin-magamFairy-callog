package org.example.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.notification.event.NotificationEvent;
import org.example.notification.model.Notification;
import org.example.notification.model.dto.NotificationDto;
import org.example.notification.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Kafka 이벤트를 알림 저장/읽음 처리/SSE 발행으로 연결하는 명령 서비스입니다.
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationCommandService {
    private final NotificationRepository notificationRepository;
    private final NotificationPolicyService policyService;
    private final NotificationSseService sseService;

    // Kafka로 받은 알림 이벤트를 중복 방지 후 저장하고 실시간 알림으로 발행합니다.
    public NotificationDto.Res createFromEvent(NotificationEvent event) {
        validateEvent(event);

        if (notificationRepository.existsByEventId(event.eventId())) {
            return notificationRepository.findByEventId(event.eventId())
                    .map(NotificationDto.Res::from)
                    .orElse(null);
        }

        if (!policyService.shouldCreate(event)) {
            return null;
        }

        Notification saved = notificationRepository.save(Notification.from(event));
        NotificationDto.Res response = NotificationDto.Res.from(saved);
        sseService.publishToUser(saved.getRecipientUserId(), response);
        return response;
    }

    // 사용자가 특정 알림 한 건을 읽음 상태로 변경합니다.
    public NotificationDto.Res markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndRecipientUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "notification not found."));
        notification.markAsRead();
        return NotificationDto.Res.from(notification);
    }

    // 사용자의 읽지 않은 모든 알림을 읽음 상태로 변경합니다.
    public void markAllAsRead(Long userId) {
        notificationRepository.findAllByRecipientUserIdAndIsReadFalse(userId)
                .forEach(Notification::markAsRead);
    }

    // 알림 생성에 반드시 필요한 이벤트 필드를 검증합니다.
    private void validateEvent(NotificationEvent event) {
        if (event == null || event.eventId() == null || event.eventId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "eventId is required.");
        }
        if (event.recipientUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recipientUserId is required.");
        }
        if (event.eventType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "eventType is required.");
        }
    }
}
