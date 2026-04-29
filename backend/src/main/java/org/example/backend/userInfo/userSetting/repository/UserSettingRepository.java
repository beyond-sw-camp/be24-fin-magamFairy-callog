package org.example.backend.userInfo.userSetting.repository;

import org.example.backend.userInfo.userSetting.model.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
    Optional<UserSetting> findByUserIdx(Long userIdx);

    Optional<UserSetting> findByUserId(String userId);
}
