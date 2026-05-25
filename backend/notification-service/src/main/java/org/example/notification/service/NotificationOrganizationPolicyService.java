package org.example.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.notification.event.NotificationEventType;
import org.example.notification.model.NotificationOrganizationPolicy;
import org.example.notification.model.dto.NotificationDto;
import org.example.notification.repository.NotificationOrganizationPolicyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;

// Gateway 사용자 컨텍스트를 기준으로 조직 알림 정책을 조회하고 수정하는 서비스입니다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationOrganizationPolicyService {
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_GENERAL_MANAGER = "ROLE_GENERAL_MANAGER";

    private final NotificationOrganizationPolicyRepository policyRepository;

    // 현재 사용자의 조직에 등록된 알림 정책 목록을 조회합니다.
    public NotificationDto.AdminPolicyRes list(Long organizationId, String role) {
        requirePolicyManager(organizationId, role);

        var policies = policyRepository
                .findAllByOrganizationIdOrderByRoleNameAscNotificationTypeAsc(organizationId)
                .stream()
                .map(NotificationDto.AdminPolicyItem::from)
                .sorted(Comparator.comparing(NotificationDto.AdminPolicyItem::roleName)
                        .thenComparing(item -> item.notificationType().name()))
                .toList();

        return new NotificationDto.AdminPolicyRes(policies);
    }

    // 현재 사용자의 조직에 알림 정책을 추가하거나 기존 정책을 수정합니다.
    @Transactional
    public NotificationDto.AdminPolicyRes update(
            Long organizationId,
            String role,
            NotificationDto.AdminPolicyReq request
    ) {
        requirePolicyManager(organizationId, role);

        if (request != null && request.policies() != null) {
            request.policies().stream()
                    .filter(item -> item != null && item.notificationType() != null)
                    .forEach(item -> upsertPolicy(organizationId, item));
        }

        return list(organizationId, role);
    }

    private void upsertPolicy(Long organizationId, NotificationDto.AdminPolicyItem item) {
        validatePolicyOrganization(organizationId, item.organizationIdx());
        String roleName = normalizeRoleName(item.roleName());
        NotificationEventType notificationType = item.notificationType();

        NotificationOrganizationPolicy policy = policyRepository
                .findByOrganizationIdAndRoleNameAndNotificationType(
                        organizationId,
                        roleName,
                        notificationType
                )
                .orElseGet(() -> policyRepository.save(NotificationOrganizationPolicy.builder()
                        .organizationId(organizationId)
                        .roleName(roleName)
                        .notificationType(notificationType)
                        .enabled(true)
                        .build()));

        policy.updateEnabled(item.enabled());
    }

    private void requirePolicyManager(Long organizationId, String role) {
        if (organizationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "organizationId is required.");
        }

        String normalizedRole = normalizeRoleName(role);
        if (!ROLE_ADMIN.equals(normalizedRole) && !ROLE_GENERAL_MANAGER.equals(normalizedRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "notification policy permission is required.");
        }
    }

    private void validatePolicyOrganization(Long currentOrganizationId, Long requestedOrganizationId) {
        if (requestedOrganizationId != null && !requestedOrganizationId.equals(currentOrganizationId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "cannot update other organization policy.");
        }
    }

    private String normalizeRoleName(String roleName) {
        return roleName == null || roleName.isBlank()
                ? NotificationOrganizationPolicy.ROLE_ALL
                : roleName.trim().toUpperCase();
    }
}
