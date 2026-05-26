package org.example.backend.common.redis;

import lombok.RequiredArgsConstructor;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * 인증 user 조회 Redis 캐시.
 * 기존 ConcurrentHashMap → Spring Cache(@Cacheable, Redis-backed)로 교체.
 * Pod 간 공유 + invalidate 가 모든 Pod에 즉시 반영.
 *
 * TTL: RedisConfig.CACHE_TTL 의 USER_AUTH 항목(60초).
 */
@Component
@RequiredArgsConstructor
public class UserAuthCache {

    private final UserRepository userRepository;

    @Cacheable(value = CacheNames.USER_AUTH, key = "#userIdx", unless = "#result == null")
    public CachedUserAuth loadUser(Long userIdx) {
        if (userIdx == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found.");
        }
        User user = userRepository.findWithOrganizationByIdx(userIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
        
        return CachedUserAuth.from(user);
    }

    @CacheEvict(value = CacheNames.USER_AUTH, key = "#userIdx")
    public void invalidate(Long userIdx) {
        // AOP 가 evict 수행
    }

    @CacheEvict(value = CacheNames.USER_AUTH, allEntries = true)
    public Optional<Integer> clearAll() {
        // @CacheEvict 가 전체 비움. 사이즈 카운트는 Redis 캐시라 즉시 알기 어려워 빈 값 반환.
        return Optional.empty();
    }
}