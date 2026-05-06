package org.example.backend.userInfo.userProfile.repository;

import org.example.backend.userInfo.userProfile.model.ProfileImageHistory;
import org.example.backend.userInfo.userProfile.model.ProfileImageHistoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileImageHistoryRepository extends JpaRepository<ProfileImageHistory, Long> {
    List<ProfileImageHistory> findByUserIdxAndHistoryTypeOrderByCreatedAtDesc(
            Long userIdx,
            ProfileImageHistoryType historyType
    );

    Optional<ProfileImageHistory> findByIdxAndUserIdx(Long idx, Long userIdx);

    long countByObjectKey(String objectKey);
}
