package org.example.backend.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * RefreshToken 을 Redis 에 저장하는 서비스.
 *
 * 기존 {@code RefreshTokenRepository}(JPA) 를 대체.
 * 핵심 이점:
 *   - TTL 자동 만료 (14일) → DB 청소 배치 불필요
 *   - 조회/삭제가 단건 인덱스 액세스보다 빠름
 *   - userId 양방향 인덱스 자동 (token → userId 가 필요하면 별도 키 추가)
 *
 * 키 설계:
 *   refresh:{userId}        = token        (사용자 → 현재 활성 토큰)
 *   refresh:token:{token}   = userId       (토큰 → 사용자) — 토큰 기반 조회 시
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenRedisService {

    private static final Duration TTL = Duration.ofDays(14);
    private static final String KEY_BY_USER  = "refresh:user:";
    private static final String KEY_BY_TOKEN = "refresh:token:";

    private final StringRedisTemplate redis;

    /** 토큰 저장. 같은 userId 의 기존 토큰은 덮어쓰기(로테이션). */
    public void save(String userId, String token) {
        // 기존 토큰의 역인덱스 정리
        String old = redis.opsForValue().get(KEY_BY_USER + userId);
        if (old != null) {
            redis.delete(KEY_BY_TOKEN + old);
        }
        redis.opsForValue().set(KEY_BY_USER + userId, token, TTL);
        redis.opsForValue().set(KEY_BY_TOKEN + token, userId, TTL);
    }

    /** userId 로 현재 활성 토큰 조회. */
    public Optional<String> findByUserId(String userId) {
        return Optional.ofNullable(redis.opsForValue().get(KEY_BY_USER + userId));
    }

    /** 토큰으로 소유자 userId 조회 (역방향). */
    public Optional<String> findUserIdByToken(String token) {
        return Optional.ofNullable(redis.opsForValue().get(KEY_BY_TOKEN + token));
    }

    /** 사용자 로그아웃 — 그 사용자의 모든 토큰 무효화. */
    public void deleteByUserId(String userId) {
        String token = redis.opsForValue().get(KEY_BY_USER + userId);
        if (token != null) {
            redis.delete(KEY_BY_TOKEN + token);
        }
        redis.delete(KEY_BY_USER + userId);
    }

    /** 특정 토큰만 무효화 (다중 디바이스 환경에서 한 디바이스만 로그아웃 등). */
    public void deleteByToken(String token) {
        String userId = redis.opsForValue().get(KEY_BY_TOKEN + token);
        redis.delete(KEY_BY_TOKEN + token);
        if (userId != null) {
            // 그 사용자가 현재 들고 있는 토큰이 이거면 user 인덱스도 삭제
            String currentToken = redis.opsForValue().get(KEY_BY_USER + userId);
            if (token.equals(currentToken)) {
                redis.delete(KEY_BY_USER + userId);
            }
        }
    }
}
