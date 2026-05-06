package org.example.backend.notification.repository;

import org.example.backend.userInfo.userProfile.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<UserProfile, Long>  {
}
