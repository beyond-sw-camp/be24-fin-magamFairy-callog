package org.example.notification.model.dto;

import lombok.Builder;
import org.example.notification.model.NotificationSetting;

// Notification 서비스의 사용자 알림 설정 API에서 사용할 DTO를 모아 둔 클래스입니다.
public class NotificationSettingDto {

    @Builder
    public record Req(
            Boolean enabled,
            Boolean taskEnabled,
            Boolean campaignEnabled,
            Boolean reviewEnabled,
            Boolean deadlineEnabled
    ) {}

    @Builder
    public record Res(
            Boolean enabled,
            Boolean taskEnabled,
            Boolean campaignEnabled,
            Boolean reviewEnabled,
            Boolean deadlineEnabled
    ) {
        public static Res from(NotificationSetting setting) {
            return Res.builder()
                    .enabled(setting.getEnabled())
                    .taskEnabled(setting.getTaskEnabled())
                    .campaignEnabled(setting.getCampaignEnabled())
                    .reviewEnabled(setting.getReviewEnabled())
                    .deadlineEnabled(setting.getDeadlineEnabled())
                    .build();
        }
    }
}