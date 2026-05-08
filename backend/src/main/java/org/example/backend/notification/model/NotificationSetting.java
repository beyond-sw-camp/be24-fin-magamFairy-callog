package org.example.backend.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.common.model.BaseEntity;
import org.example.backend.user.model.User;

@Entity
@Table(
        name = "notification_settings",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_setting_user", columnNames = "user_idx")
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NotificationSetting extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_idx", nullable = false)
    private User user;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationLevel level = NotificationLevel.NORMAL;

    @Builder.Default
    @Column(nullable = false)
    private Boolean inAppEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean emailEnabled = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean browserEnabled = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean taskAssignedEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean taskStatusChangedEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean qaReviewEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean deadlineEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean campaignEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean scheduleEnabled = true;

    public static NotificationSetting defaultFor(User user) {
        return NotificationSetting.builder()
                .user(user)
                .build();
    }

    public void update(NotificationDto.SettingReq request) {
        if (request == null) {
            return;
        }

        if (request.enabled() != null) {
            this.enabled = request.enabled();
        }
        if (request.level() != null) {
            this.level = request.level();
        }
        if (request.methods() != null) {
            NotificationDto.NotificationMethods methods = request.methods();
            if (methods.inApp() != null) {
                this.inAppEnabled = methods.inApp();
            }
            if (methods.email() != null) {
                this.emailEnabled = methods.email();
            }
            if (methods.browser() != null) {
                this.browserEnabled = methods.browser();
            }
        }
        if (request.conditions() != null) {
            NotificationDto.NotificationConditions conditions = request.conditions();
            if (conditions.taskAssigned() != null) {
                this.taskAssignedEnabled = conditions.taskAssigned();
            }
            if (conditions.taskStatusChanged() != null) {
                this.taskStatusChangedEnabled = conditions.taskStatusChanged();
            }
            if (conditions.qaReview() != null) {
                this.qaReviewEnabled = conditions.qaReview();
            }
            if (conditions.deadline() != null) {
                this.deadlineEnabled = conditions.deadline();
            }
            if (conditions.campaign() != null) {
                this.campaignEnabled = conditions.campaign();
            }
            if (conditions.schedule() != null) {
                this.scheduleEnabled = conditions.schedule();
            }
        }
    }
}
