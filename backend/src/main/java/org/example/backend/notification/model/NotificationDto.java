package org.example.backend.notification.model;

import org.example.backend.user.model.User;

import java.util.Date;
import java.util.List;

public class NotificationDto {
    public record ListRes(
            List<Res> notifications,
            long unreadCount
    ) {}

    public record Res(
            Long idx,
            Long id,
            String type,
            String category,
            String severity,
            String title,
            String message,
            String detail,
            Date createdAt,
            Boolean isRead,
            String source,
            String targetLabel,
            String targetUrl
    ) {
        public static Res from(Notification notification) {
            User sender = notification.getSender();

            return new Res(
                    notification.getIdx(),
                    notification.getIdx(),
                    notification.getType().name(),
                    categoryOf(notification.getType()),
                    notification.getSeverity().name().toLowerCase(),
                    notification.getTitle(),
                    notification.getMessage(),
                    notification.getDetail(),
                    notification.getCreatedAt(),
                    Boolean.TRUE.equals(notification.getIsRead()),
                    sender != null ? sender.getName() : "System",
                    notification.getTargetLabel(),
                    notification.getTargetUrl()
            );
        }
    }

    public record CreateReq(
            Long recipientIdx,
            NotificationType type,
            NotificationSeverity severity,
            String title,
            String message,
            String detail,
            String targetLabel,
            String targetUrl
    ) {}

    private static String categoryOf(NotificationType type) {
        return switch (type) {
            case TASK_ASSIGNED, TASK_STATUS_CHANGED -> "task";
            case REVIEW_REQUESTED, REVIEW_APPROVED, REVIEW_REJECTED -> "qa";
            case CAMPAIGN_INVITED, CAMPAIGN_MEMBER_ADDED -> "campaign";
            case SYSTEM -> "system";
        };
    }
}
