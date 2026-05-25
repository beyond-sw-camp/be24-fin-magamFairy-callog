package org.example.notification.deadline;

import java.time.LocalDateTime;
import java.util.List;

// 마감 알림 대상 업무와 수신자를 조회하는 읽기 전용 포트입니다.
public interface DeadlineNotificationCandidateRepository {

    List<DeadlineTaskCandidate> findTasksDueBetween(LocalDateTime start, LocalDateTime end);

    List<DeadlineTaskCandidate> findOverdueTasks(LocalDateTime now);

    List<DeadlineRecipient> findRecipients(DeadlineTaskCandidate task);
}
