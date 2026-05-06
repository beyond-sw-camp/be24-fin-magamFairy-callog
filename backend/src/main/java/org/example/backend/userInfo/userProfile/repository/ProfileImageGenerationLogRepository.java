package org.example.backend.userInfo.userProfile.repository;

import org.example.backend.userInfo.userProfile.model.ProfileImageGenerationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileImageGenerationLogRepository extends JpaRepository<ProfileImageGenerationLog, Long> {
}
