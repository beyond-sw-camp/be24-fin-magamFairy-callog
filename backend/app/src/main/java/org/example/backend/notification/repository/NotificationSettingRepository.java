package org.example.backend.notification.repository;

import org.example.backend.notification.model.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    Optional<NotificationSetting> findByUser_Idx(Long userIdx);
}
