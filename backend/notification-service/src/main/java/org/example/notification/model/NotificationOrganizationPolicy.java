package org.example.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.notification.event.NotificationEventType;

@Entity
@Table(
        name = "notification_organization_policies",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_organization_policy",
                columnNames = {"organization_id", "role_name", "notification_type"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
// Notification 서비스에서 조직/역할 단위 알림 허용 정책을 저장하는 엔티티입니다.
public class NotificationOrganizationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "role_name", nullable = false, length = 40)
    private String roleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationEventType notificationType;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;
}
