package org.example.backend.user.service;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    private static final String PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redis;

    public void blacklist(String token, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) return;   // 이미 만료면 무의미
        redis.opsForValue().set(PREFIX + key(token), "1", ttl);
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + key(token)));
    }

    /** 토큰이 길어서 고정 길이 해시로 키화 (충돌무시 캐시키 용도 MD5면 충분). */
    private String key(String token) {
        return DigestUtils.md5DigestAsHex(token.getBytes(StandardCharsets.UTF_8));
    }
}
