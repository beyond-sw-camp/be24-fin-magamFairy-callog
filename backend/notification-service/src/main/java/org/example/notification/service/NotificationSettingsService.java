package org.example.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.notification.model.NotificationSetting;
import org.example.notification.model.dto.NotificationSettingDto;
import org.example.notification.repository.NotificationSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 사용자별 알림 수신 설정을 조회하고 수정하는 서비스입니다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationSettingsService {
    private final NotificationSettingRepository settingRepository;

    // 사용자 알림 설정을 조회하고 없으면 기본 설정을 생성합니다.
    @Transactional
    public NotificationSettingDto.Res get(Long userId) {
        NotificationSetting setting = settingRepository.findByUserId(userId)
                .orElseGet(() -> settingRepository.save(NotificationSetting.defaultFor(userId)));
        return NotificationSettingDto.Res.from(setting);
    }

    // 사용자 알림 설정을 요청 값으로 부분 수정합니다.
    @Transactional
    public NotificationSettingDto.Res update(Long userId, NotificationSettingDto.Req request) {
        NotificationSetting setting = settingRepository.findByUserId(userId)
                .orElseGet(() -> settingRepository.save(NotificationSetting.defaultFor(userId)));
        if (request != null) {
            setting.update(request);
        }
        return NotificationSettingDto.Res.from(setting);
    }
}
