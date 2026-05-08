package org.example.backend.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.common.model.BaseEntity;
import org.example.backend.user.model.User;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_recipient_read", columnList = "recipient_idx,is_read"),
                @Index(name = "idx_notifications_recipient_created", columnList = "recipient_idx,create_date"),
                @Index(name = "idx_notifications_dedupe", columnList = "dedupe_key")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Notification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_idx", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_idx")
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationSeverity severity;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(length = 120)
    private String targetLabel;

    @Column(length = 500)
    private String targetUrl;

    @Column(name = "dedupe_key", unique = true, length = 180)
    private String dedupeKey;

    @Column(length = 60)
    private String referenceType;

    private Long referenceId;

    @Column(length = 40)
    private String referenceStatus;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    private LocalDateTime readAt;

    public void markAsRead() {
        if (Boolean.TRUE.equals(isRead)) {
            return;
        }

        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    public void updateReferenceStatus(String referenceStatus) {
        this.referenceStatus = referenceStatus;
    }
}
