package org.example.backend.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisLock {

    private final StringRedisTemplate redis;

    // 내가 건 락만 안전하게 해제 (값 일치할 때만 DEL)
    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] " +
                    "then return redis.call('del', KEYS[1]) else return 0 end";

    /** 락 획득 시 토큰 반환(해제용), 실패 시 null. ttl 은 작업 최대 소요시간보다 길게. */
    public String tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean isOk = redis.opsForValue().setIfAbsent(key, token, ttl);

        return Boolean.TRUE.equals(isOk) ? token : null;
    }

    public void unLock(String key, String token) {
        redis.execute(
                RedisScript.of(UNLOCK_LUA, Long.class),
                List.of(key), token);
    }
}
