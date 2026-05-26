package org.example.backend.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component("dashboardCacheVersionService")
@RequiredArgsConstructor
public class DashboardCacheVersionService {

    private static final String DASHBOARD_GLOBAL_VERSION = "cache_ver:dashboard:global";
    private static final String DASHBOARD_USER_VERSION = "cache_ver:dashboard:user:";

    /** 전역 버전 키는 1년 TTL (사실상 영구적이지만 메모리 보호) */
    private static final Duration GLOBAL_TTL = Duration.ofDays(365);
    /** 유저별 버전 키는 60일 TTL (비활성 유저 자동 정리) */
    private static final Duration USER_TTL = Duration.ofDays(60);

    private final StringRedisTemplate redis;

    public String getVersion(Long userIdx) {
        // ⚡ MGET으로 전역/유저 버전을 1번의 Redis 왕복으로 조회 (기존 2-RTT → 1-RTT)
        java.util.List<String> values = redis.opsForValue().multiGet(
                java.util.List.of(DASHBOARD_GLOBAL_VERSION, DASHBOARD_USER_VERSION + userIdx)
        );
        long global = parseVersion(values == null || values.isEmpty() ? null : values.get(0));
        long user   = parseVersion(values == null || values.size() < 2 ? null : values.get(1));
        return global + ":" + user;
    }

    private long parseVersion(String value) {
        if (value == null) return 1L;
        try { return Long.parseLong(value); } catch (NumberFormatException e) { return 1L; }
    }

    public long getGlobalVersion() {
        return getVersionValue(DASHBOARD_GLOBAL_VERSION);
    }

    public long getUserVersion(Long userIdx) {
        return getVersionValue(DASHBOARD_USER_VERSION + userIdx);
    }

    public void increaseGlobalVersion() {
        Long val = redis.opsForValue().increment(DASHBOARD_GLOBAL_VERSION);
        // 새로 생성된 키(val==1)이면 TTL 설정. 기존 키라도 TTL이 없으면 설정.
        if (val != null && (val == 1L || redis.getExpire(DASHBOARD_GLOBAL_VERSION) < 0)) {
            redis.expire(DASHBOARD_GLOBAL_VERSION, GLOBAL_TTL);
        }
    }

    public void increaseUserVersion(Long userIdx) {
        if (userIdx == null) {
            return;
        }

        String key = DASHBOARD_USER_VERSION + userIdx;
        Long val = redis.opsForValue().increment(key);
        // 새로 생성된 키이거나 기존 키에 TTL이 없으면 TTL 설정
        if (val != null && (val == 1L || redis.getExpire(key) < 0)) {
            redis.expire(key, USER_TTL);
        }
    }

    private long getVersionValue(String key) {
        String value = redis.opsForValue().get(key);

        if (value == null) {
            Boolean created = redis.opsForValue().setIfAbsent(key, "1", USER_TTL);
            if (Boolean.TRUE.equals(created)) {
                return 1L;
            }

            value = redis.opsForValue().get(key);

            if (value == null) {
                return 1L;
            }
        }

        return Long.parseLong(value);
    }
}
