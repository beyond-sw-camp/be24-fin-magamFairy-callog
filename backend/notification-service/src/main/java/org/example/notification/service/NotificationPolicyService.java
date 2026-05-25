package org.example.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.notification.event.NotificationEvent;
import org.example.notification.event.NotificationSeverity;
import org.example.notification.model.NotificationOrganizationPolicy;
import org.example.notification.model.NotificationSetting;
import org.example.notification.repository.NotificationOrganizationPolicyRepository;
import org.example.notification.repository.NotificationSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 조직 정책과 개인 설정을 기준으로 알림 생성 가능 여부를 판단하는 서비스입니다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationPolicyService {
    private final NotificationSettingRepository settingRepository;
    private final NotificationOrganizationPolicyRepository policyRepository;

    // 알림 이벤트가 실제 알림으로 저장되어야 하는지 최종 판단합니다.
    public boolean shouldCreate(NotificationEvent event) {
        if (event == null || event.recipientUserId() == null) return false;
        if (event.severity() == NotificationSeverity.CRITICAL) return true;
        return allowedByOrganizationPolicy(event) && allowedByUserSetting(event);
    }

    // 조직/역할 단위 정책에서 해당 알림 유형이 허용되는지 확인합니다.
    private boolean allowedByOrganizationPolicy(NotificationEvent event) {
        if (event.organizationId() == null || event.eventType() == null) return true;

        String recipientRole = normalizeRole(event.recipientRole());
        List<String> roles = List.of(recipientRole, NotificationOrganizationPolicy.ROLE_ALL);

        List<NotificationOrganizationPolicy> policies =
                policyRepository.findAllByOrganizationIdAndRoleNameInAndNotificationType(
                        event.organizationId(), roles, event.eventType());

        return policies.stream()
                .filter(p -> recipientRole.equals(p.getRoleName()))
                .findFirst()
                .or(() -> policies.stream()
                        .filter(p -> NotificationOrganizationPolicy.ROLE_ALL.equals(p.getRoleName()))
                        .findFirst())
                .map(NotificationOrganizationPolicy::getEnabled)
                .orElse(true);
    }

    // 사용자 개인 알림 설정에서 해당 알림 유형이 허용되는지 확인합니다.
    private boolean allowedByUserSetting(NotificationEvent event) {
        NotificationSetting setting = settingRepository.findByUserId(event.recipientUserId())
                .orElseGet(() -> NotificationSetting.defaultFor(event.recipientUserId()));

        if (!Boolean.TRUE.equals(setting.getEnabled())) return false;

        return switch (event.eventType()) {
            case TASK_ASSIGNED, TASK_STATUS_CHANGED, TASK_UPDATED -> Boolean.TRUE.equals(setting.getTaskEnabled());
            case CAMPAIGN_INVITED, CAMPAIGN_INVITATION_ACCEPTED, CAMPAIGN_INVITATION_REJECTED, CAMPAIGN_MEMBER_ADDED -> Boolean.TRUE.equals(setting.getCampaignEnabled());
            case REVIEW_REQUESTED, REVIEW_APPROVED, REVIEW_REJECTED -> Boolean.TRUE.equals(setting.getReviewEnabled());
            case DEADLINE_24H, DEADLINE_1H, DEADLINE_OVERDUE -> Boolean.TRUE.equals(setting.getDeadlineEnabled());
            case SYSTEM -> true;
        };
    }

    // 역할명이 비어 있으면 전체 역할 정책인 ALL로 정규화합니다.
    private String normalizeRole(String role) {
        return role == null || role.isBlank() ? NotificationOrganizationPolicy.ROLE_ALL : role.trim().toUpperCase();
    }
}
