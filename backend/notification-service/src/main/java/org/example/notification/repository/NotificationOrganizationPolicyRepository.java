package org.example.notification.repository;

import org.example.notification.event.NotificationEventType;
import org.example.notification.model.NotificationOrganizationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationOrganizationPolicyRepository
        extends JpaRepository<NotificationOrganizationPolicy, Long> {

    Optional<NotificationOrganizationPolicy> findByOrganizationIdAndRoleNameAndNotificationType(
            Long organizationId,
            String roleName,
            NotificationEventType notificationEventType
    );
}
