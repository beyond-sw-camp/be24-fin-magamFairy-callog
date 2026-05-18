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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.common.model.BaseEntity;
import org.example.backend.organization.model.Organization;

@Entity
@Table(
        name = "notification_admin_policies",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_admin_policy",
                columnNames = {"organization_idx", "role_name", "notification_type"}
        ),
        indexes = @Index(name = "idx_notification_admin_policy_lookup", columnList = "organization_idx,role_name")
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NotificationAdminPolicy extends BaseEntity {
    public static final String ROLE_ALL = "ALL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_idx", nullable = false)
    private Organization organization;

    @Column(name = "role_name", nullable = false, length = 40)
    private String roleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    public void updateEnabled(Boolean nextEnabled) {
        this.enabled = nextEnabled == null || nextEnabled;
    }
}
