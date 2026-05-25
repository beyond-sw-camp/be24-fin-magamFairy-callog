package org.example.notification.repository;

import org.example.notification.event.NotificationEventType;
import org.example.notification.model.NotificationOrganizationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationOrganizationPolicyRepository
        extends JpaRepository<NotificationOrganizationPolicy, Long> {

    Optional<NotificationOrganizationPolicy> findByOrganizationIdAndRoleNameAndNotificationType(
            Long organizationId,
            String roleName,
            NotificationEventType notificationEventType
    );

    List<NotificationOrganizationPolicy> findAllByOrganizationIdOrderByRoleNameAscNotificationTypeAsc(
            Long organizationId
    );

    List<NotificationOrganizationPolicy> findAllByOrganizationIdAndRoleNameInAndNotificationType(
            Long organizationId,
            Collection<String> roleNames,
            NotificationEventType notificationType
    );
}
