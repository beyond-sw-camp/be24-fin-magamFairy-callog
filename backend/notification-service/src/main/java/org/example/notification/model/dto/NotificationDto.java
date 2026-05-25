package org.example.notification.model.dto;

import org.example.notification.model.Notification;

import java.time.Instant;
import java.util.List;

// Notification 서비스의 알림 조회/변경 API에서 사용할 DTO를 모아 둔 클래스입니다.
public class NotificationDto {

    public record ListRes(
            List<Res> notifications,
            long unreadCount
    ) {}

    public record Res(
            Long idx,
            String type,
            String severity,
            String title,
            String message,
            String detail,
            Instant createdAt,
            Boolean isRead,
            String senderName,
            String targetLabel,
            String targetUrl,
            String referenceType,
            Long referenceId,
            String referenceStatus
    ) {
        public static Res from(Notification notification) {
            return new Res(
                    notification.getId(),
                    notification.getType().name(),
                    notification.getSeverity().name(),
                    notification.getTitle(),
                    notification.getMessage(),
                    notification.getDetail(),
                    notification.getCreatedAt(),
                    notification.getIsRead(),
                    notification.getSenderName(),
                    notification.getTargetLabel(),
                    notification.getTargetUrl(),
                    notification.getReferenceType() == null ? null : notification.getReferenceType().name(),
                    notification.getReferenceId(),
                    notification.getReferenceStatus()
            );
        }
    }
}
