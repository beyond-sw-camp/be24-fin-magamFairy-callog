package org.example.notification.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

// 여러 Pod 중 하나만 특정 작업을 실행하도록 Redis에 잠금을 거는 컴포넌트입니다.
@Component
@RequiredArgsConstructor
public class RedisLock {
    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] " +
                    "then return redis.call('del', KEYS[1]) else return 0 end";

    private final StringRedisTemplate redisTemplate;

    // 락을 잡으면 고유 토큰을 반환하고, 이미 다른 Pod가 잡고 있으면 null을 반환합니다.
    public String tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(locked) ? token : null;
    }

    // 내가 잡은 락일 때만 해제합니다.
    public void unLock(String key, String token) {
        redisTemplate.execute(
                RedisScript.of(UNLOCK_LUA, Long.class),
                List.of(key),
                token
        );
    }
}
