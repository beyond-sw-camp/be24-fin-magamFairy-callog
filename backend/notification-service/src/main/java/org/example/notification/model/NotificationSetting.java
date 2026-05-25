package org.example.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.example.notification.model.dto.NotificationSettingDto;

@Table(
        name = "notification_settings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_setting_user",
                columnNames = "user_id"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
// Notification 서비스에서 사용자별 알림 수신 설정을 저장하는 엔티티입니다.
public class NotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String level = "NORMAL";

    @Builder.Default
    @Column(nullable = false)
    private Boolean taskEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean campaignEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean reviewEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean deadlineEnabled = true;

    public static NotificationSetting defaultFor(Long userId) {
        return NotificationSetting.builder()
                .userId(userId)
                .build();
    }

    public void update(NotificationSettingDto.Req req) {
        if (req.enabled() != null) this.enabled = req.enabled();
        if (req.taskEnabled() != null) this.taskEnabled = req.taskEnabled();
        if (req.campaignEnabled() != null) this.campaignEnabled = req.campaignEnabled();
        if (req.reviewEnabled() != null) this.reviewEnabled = req.reviewEnabled();
        if (req.deadlineEnabled() != null) this.deadlineEnabled = req.deadlineEnabled();
    }
}
