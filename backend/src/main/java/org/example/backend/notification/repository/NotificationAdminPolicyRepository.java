package org.example.backend.notification.repository;

import org.example.backend.notification.model.NotificationAdminPolicy;
import org.example.backend.notification.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationAdminPolicyRepository extends JpaRepository<NotificationAdminPolicy, Long> {
    List<NotificationAdminPolicy> findAllByOrganization_IdxOrderByRoleNameAscNotificationTypeAsc(Long organizationIdx);

    Optional<NotificationAdminPolicy> findByOrganization_IdxAndRoleNameAndNotificationType(
            Long organizationIdx,
            String roleName,
            NotificationType notificationType
    );

    List<NotificationAdminPolicy> findAllByOrganization_IdxAndRoleNameInAndNotificationType(
            Long organizationIdx,
            Collection<String> roleNames,
            NotificationType notificationType
    );
}
