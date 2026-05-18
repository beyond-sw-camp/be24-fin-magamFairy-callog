package org.example.backend.userInfo.repository;

import org.example.backend.userInfo.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserIdx(Long userIdx);

    Optional<UserProfile> findByUserId(String userId);
}
