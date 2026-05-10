package org.example.backend.notification.model;

import org.example.backend.user.model.User;

import java.util.Date;
import java.util.List;

public class NotificationDto {
    public record ListRes(
            List<Res> notifications,
            long unreadCount
    ) {}

    public record Res(
            Long idx,
            Long id,
            String type,
            String category,
            String severity,
            String title,
            String message,
            String detail,
            Date createdAt,
            Boolean isRead,
            String source,
            String targetLabel,
            String targetUrl,
            String referenceType,
            Long referenceId,
            String referenceStatus
    ) {
        public static Res from(Notification notification) {
            User sender = notification.getSender();

            return new Res(
                    notification.getIdx(),
                    notification.getIdx(),
                    notification.getType().name(),
                    categoryOf(notification.getType()),
                    notification.getSeverity().name().toLowerCase(),
                    notification.getTitle(),
                    notification.getMessage(),
                    notification.getDetail(),
                    notification.getCreatedAt(),
                    Boolean.TRUE.equals(notification.getIsRead()),
                    sender != null ? sender.getName() : "System",
                    notification.getTargetLabel(),
                    notification.getTargetUrl(),
                    notification.getReferenceType(),
                    notification.getReferenceId(),
                    notification.getReferenceStatus()
            );
        }
    }

    public record CreateReq(
            Long recipientIdx,
            NotificationType type,
            NotificationSeverity severity,
            String title,
            String message,
            String detail,
            String targetLabel,
            String targetUrl
    ) {}

    public record NotificationMethods(
            Boolean inApp,
            Boolean email,
            Boolean browser
    ) {}

    public record NotificationConditions(
            Boolean taskAssigned,
            Boolean taskStatusChanged,
            Boolean qaReview,
            Boolean deadline,
            Boolean campaign,
            Boolean schedule
    ) {}

    public record SettingReq(
            Boolean enabled,
            NotificationLevel level,
            NotificationMethods methods,
            NotificationConditions conditions
    ) {}

    public record SettingRes(
            Boolean enabled,
            NotificationLevel level,
            NotificationMethods methods,
            NotificationConditions conditions
    ) {
        public static SettingRes from(NotificationSetting setting) {
            return new SettingRes(
                    setting.getEnabled(),
                    setting.getLevel(),
                    new NotificationMethods(
                            setting.getInAppEnabled(),
                            setting.getEmailEnabled(),
                            setting.getBrowserEnabled()
                    ),
                    new NotificationConditions(
                            setting.getTaskAssignedEnabled(),
                            setting.getTaskStatusChangedEnabled(),
                            setting.getQaReviewEnabled(),
                            setting.getDeadlineEnabled(),
                            setting.getCampaignEnabled(),
                            setting.getScheduleEnabled()
                    )
            );
        }
    }

    public record AdminPolicyItem(
            Long idx,
            Long organizationIdx,
            String roleName,
            NotificationType notificationType,
            Boolean enabled
    ) {
        public static AdminPolicyItem from(NotificationAdminPolicy policy) {
            return new AdminPolicyItem(
                    policy.getIdx(),
                    policy.getOrganization().getIdx(),
                    policy.getRoleName(),
                    policy.getNotificationType(),
                    policy.getEnabled()
            );
        }
    }

    public record AdminPolicyReq(List<AdminPolicyItem> policies) {}

    public record AdminPolicyRes(List<AdminPolicyItem> policies) {}

    private static String categoryOf(NotificationType type) {
        return switch (type) {
            case TASK_ASSIGNED, TASK_STATUS_CHANGED, TASK_UPDATED -> "task";
            case REVIEW_REQUESTED, REVIEW_APPROVED, REVIEW_REJECTED -> "qa";
            case DEADLINE_24H, DEADLINE_1H, DEADLINE_OVERDUE -> "schedule";
            case CAMPAIGN_INVITED, CAMPAIGN_INVITATION_ACCEPTED, CAMPAIGN_INVITATION_REJECTED,
                    CAMPAIGN_MEMBER_ADDED -> "campaign";
            case SYSTEM -> "system";
        };
    }
}
