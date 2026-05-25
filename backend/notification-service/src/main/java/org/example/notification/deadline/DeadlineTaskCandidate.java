package org.example.notification.deadline;

import java.time.LocalDateTime;

// 마감 알림 대상으로 조회된 업무의 최소 정보입니다.
public record DeadlineTaskCandidate(
        Long taskId,
        String taskName,
        LocalDateTime dueDate,
        Long campaignId
) {
}
