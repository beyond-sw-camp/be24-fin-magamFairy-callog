package org.example.backend.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component("dashboardCacheVersionService")
@RequiredArgsConstructor
public class DashboardCacheVersionService {

    private static final String DASHBOARD_GLOBAL_VERSION = "cache_ver:dashboard:global";
    private static final String DASHBOARD_USER_VERSION = "cache_ver:dashboard:user:";

    private final StringRedisTemplate redis;

    public String getVersion(Long userIdx) {
        return getGlobalVersion() + ":" + getUserVersion(userIdx);
    }

    public long getGlobalVersion() {
        return getVersionValue(DASHBOARD_GLOBAL_VERSION);
    }

    public long getUserVersion(Long userIdx) {
        return getVersionValue(DASHBOARD_USER_VERSION + userIdx);
    }

    public void increaseGlobalVersion() {
        redis.opsForValue().increment(DASHBOARD_GLOBAL_VERSION);
    }

    public void increaseUserVersion(Long userIdx) {
        if (userIdx == null) {
            return;
        }

        redis.opsForValue().increment(DASHBOARD_USER_VERSION + userIdx);
    }

    private long getVersionValue(String key) {
        String value = redis.opsForValue().get(key);

        if (value == null) {
            Boolean created = redis.opsForValue().setIfAbsent(key, "1");
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
