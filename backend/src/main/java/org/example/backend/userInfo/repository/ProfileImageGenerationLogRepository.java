package org.example.backend.userInfo.repository;

import org.example.backend.userInfo.model.ProfileImageGenerationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileImageGenerationLogRepository extends JpaRepository<ProfileImageGenerationLog, Long> {
}
