package org.example.backend.notification.repository;

import org.example.backend.notification.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByRecipient_IdxOrderByCreatedAtDesc(Long recipientIdx);

    List<Notification> findAllByRecipient_IdxOrderByCreatedAtDesc(Long recipientIdx, Pageable pageable);

    List<Notification> findAllByRecipient_IdxAndIsReadFalse(Long recipientIdx);

    Optional<Notification> findByIdxAndRecipient_Idx(Long idx, Long recipientIdx);

    Optional<Notification> findByDedupeKey(String dedupeKey);

    boolean existsByDedupeKey(String dedupeKey);

    List<Notification> findAllByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

    long countByRecipient_IdxAndIsReadFalse(Long recipientIdx);
}
