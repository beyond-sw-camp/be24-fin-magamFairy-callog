package org.example.notification.deadline;

// 마감 알림을 받을 사용자와 정책 판단에 필요한 최소 정보입니다.
public record DeadlineRecipient(
        Long userId,
        Long organizationId,
        String role
) {
}
