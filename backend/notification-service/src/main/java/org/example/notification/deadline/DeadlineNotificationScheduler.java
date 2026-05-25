package org.example.notification.deadline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notification.common.redis.RedisLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

// RedisLock으로 여러 Pod 중 하나만 마감 알림 배치를 실행하게 하는 스케줄러입니다.
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "notification.deadline.scheduler.enabled",
        havingValue = "true"
)
public class DeadlineNotificationScheduler {
    private static final String LOCK_KEY = "lock:notification:deadline";
    private static final Duration LOCK_TTL = Duration.ofMinutes(4);

    private final DeadlineNotificationService deadlineNotificationService;
    private final RedisLock redisLock;

    // 5분마다 마감 알림 발행을 시도하고, 락을 잡은 Pod만 실제 작업을 실행합니다.
    @Scheduled(fixedRateString = "${notification.deadline.scheduler.fixed-rate-ms:300000}")
    public void publishDeadlineNotifications() {
        String token = redisLock.tryLock(LOCK_KEY, LOCK_TTL);
        if (token == null) {
            log.debug("Deadline notification scheduler skipped because another pod holds the lock.");
            return;
        }

        try {
            deadlineNotificationService.publishDeadlineNotifications();
        } finally {
            redisLock.unLock(LOCK_KEY, token);
        }
    }
}
