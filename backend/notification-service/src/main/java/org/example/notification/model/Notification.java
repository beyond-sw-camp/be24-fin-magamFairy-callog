package org.example.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.notification.event.NotificationEvent;
import org.example.notification.event.NotificationEventType;
import org.example.notification.event.NotificationReferenceType;
import org.example.notification.event.NotificationSeverity;

import java.time.Instant;

@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_recipient_read", columnList = "recipient_user_id,is_read"),
                @Index(name = "idx_notification_recipient_created", columnList = "recipient_user_id,created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notifications_event_id", columnNames = "event_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
// Notification 서비스가 자체 DB에 저장할 알림 한 건을 표현하는 엔티티입니다.
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 80)
    private String eventId;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(name = "sender_user_id")
    private Long senderUserId;

    @Column(name = "sender_name")
    private String senderName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationEventType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationSeverity severity;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String detail;

    private String targetLabel;
    private String targetUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private NotificationReferenceType referenceType;

    private Long referenceId;
    private String referenceStatus;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false)
    private Instant createdAt;

    public static Notification from(NotificationEvent event) {
        return Notification.builder()
                .eventId(event.eventId())
                .recipientUserId(event.recipientUserId())
                .senderUserId(event.senderUserId())
                .senderName(event.senderName())
                .type(event.eventType())
                .severity(event.severity() == null ? NotificationSeverity.NORMAL : event.severity())
                .title(event.title())
                .message(event.message())
                .detail(event.detail())
                .targetLabel(event.targetLabel())
                .targetUrl(event.targetUrl())
                .referenceType(event.referenceType())
                .referenceId(event.referenceId())
                .referenceStatus(event.referenceStatus())
                .createdAt(Instant.now())
                .build();
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
