package org.example.notification.kafka;

import org.example.notification.event.NotificationEvent;

import java.time.Instant;

// Kafka 알림 이벤트 처리 실패 내용을 DLT에 보관하기 위한 메시지입니다.
public record NotificationDeadLetterEvent(
        NotificationEvent event,
        String reason,
        String exceptionType,
        Instant failedAt
) {
    public static NotificationDeadLetterEvent from(NotificationEvent event, Exception exception) {
        String reason = exception == null || exception.getMessage() == null
                ? "unknown"
                : exception.getMessage();
        String exceptionType = exception == null
                ? "unknown"
                : exception.getClass().getName();
        return new NotificationDeadLetterEvent(event, reason, exceptionType, Instant.now());
    }
}
