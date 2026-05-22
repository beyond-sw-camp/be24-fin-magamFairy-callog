package org.example.backend.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.notification.model.NotificationAdminPolicy;
import org.example.backend.notification.model.NotificationLevel;
import org.example.backend.notification.model.NotificationSetting;
import org.example.backend.notification.model.NotificationSeverity;
import org.example.backend.notification.model.NotificationType;
import org.example.backend.notification.repository.NotificationAdminPolicyRepository;
import org.example.backend.notification.repository.NotificationSettingRepository;
import org.example.backend.user.model.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationPreferenceResolver {
    private final NotificationSettingRepository settingRepository;
    private final NotificationAdminPolicyRepository adminPolicyRepository;

    public boolean shouldCreate(User recipient, NotificationType type, NotificationSeverity severity) {
        if (recipient == null || recipient.getIdx() == null) {
            return false;
        }

        if (severity == NotificationSeverity.CRITICAL) {
            return true;
        }

        if (!isAllowedByAdminPolicy(recipient, type)) {
            return false;
        }

        NotificationSetting setting = settingRepository.findByUser_Idx(recipient.getIdx())
                .orElseGet(() -> NotificationSetting.defaultFor(recipient));

        if (!Boolean.TRUE.equals(setting.getEnabled()) || !Boolean.TRUE.equals(setting.getInAppEnabled())) {
            return false;
        }

        return isAllowedByCondition(setting, type) && isAllowedByLevel(setting.getLevel(), severity);
    }

    private boolean isAllowedByAdminPolicy(User recipient, NotificationType type) {
        if (recipient.getOrganization() == null || type == null) {
            return true;
        }

        List<String> roleNames = List.of(
                normalizeRole(recipient.getRole()),
                NotificationAdminPolicy.ROLE_ALL
        );

        List<NotificationAdminPolicy> policies = adminPolicyRepository
                .findAllByOrganization_IdxAndRoleNameInAndNotificationType(
                        recipient.getOrganization().getIdx(),
                        roleNames,
                        type
                );
        return policies.stream()
                .filter(policy -> normalizeRole(recipient.getRole()).equals(policy.getRoleName()))
                .findFirst()
                .or(() -> policies.stream()
                        .filter(policy -> NotificationAdminPolicy.ROLE_ALL.equals(policy.getRoleName()))
                        .findFirst())
                .map(NotificationAdminPolicy::getEnabled)
                .orElse(true);
    }

    private boolean isAllowedByCondition(NotificationSetting setting, NotificationType type) {
        if (type == null) {
            return true;
        }

        return switch (type) {
            case TASK_ASSIGNED -> Boolean.TRUE.equals(setting.getTaskAssignedEnabled());
            case TASK_STATUS_CHANGED, TASK_UPDATED -> Boolean.TRUE.equals(setting.getTaskStatusChangedEnabled());
            case REVIEW_REQUESTED, REVIEW_APPROVED, REVIEW_REJECTED,
                    AI_JUDGE_COMPLETED, AI_JUDGE_REVIEW_REQUIRED, AI_JUDGE_FAILED ->
                    Boolean.TRUE.equals(setting.getQaReviewEnabled());
            case DEADLINE_24H, DEADLINE_1H, DEADLINE_OVERDUE -> Boolean.TRUE.equals(setting.getDeadlineEnabled());
            case CAMPAIGN_INVITED, CAMPAIGN_INVITATION_ACCEPTED, CAMPAIGN_INVITATION_REJECTED,
                    CAMPAIGN_MEMBER_ADDED -> Boolean.TRUE.equals(setting.getCampaignEnabled());
            case SYSTEM -> true;
        };
    }

    private boolean isAllowedByLevel(NotificationLevel level, NotificationSeverity severity) {
        NotificationLevel nextLevel = level == null ? NotificationLevel.NORMAL : level;
        NotificationSeverity nextSeverity = severity == null ? NotificationSeverity.NORMAL : severity;

        return switch (nextLevel) {
            case ESSENTIAL -> nextSeverity == NotificationSeverity.HIGH;
            case NORMAL -> nextSeverity != NotificationSeverity.LOW;
            case ALL -> true;
        };
    }

    private String normalizeRole(String role) {
        return role == null || role.isBlank()
                ? NotificationAdminPolicy.ROLE_ALL
                : role.trim().toUpperCase();
    }
}
