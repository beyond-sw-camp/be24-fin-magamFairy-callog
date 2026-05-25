package org.example.notification.repository;

import org.example.notification.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByEventId(String eventId);

    List<Notification> findAllByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId, Pageable pageable);

    Optional<Notification> findByIdAndRecipientUserId(Long id, Long recipientUserId);

    List<Notification> findAllByRecipientUserIdAndIsReadFalse(Long recipientUserId);

    long countByRecipientUserIdAndIsReadFalse(Long recipientUserId);

    // eventId로 중복 저장 막기 (멱등성)
    Optional<Notification> findByEventId(String eventId);
}
