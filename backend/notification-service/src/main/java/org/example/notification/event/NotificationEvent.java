package org.example.notification.event;

public record NotificationEvent(
        String eventId,
        NotificationEventType eventType,
        Long recipientUserId,
        Long organizationId,
        String recipientRole,
        Long senderUserId,
        String senderName,
        String title,
        String message,
        String detail,
        String targetLabel,
        String targetUrl,
        NotificationReferenceType referenceType,
        Long referenceId,
        String referenceStatus,
        NotificationSeverity severity,
        String occurredAt,
        String schemaVersion
) {}