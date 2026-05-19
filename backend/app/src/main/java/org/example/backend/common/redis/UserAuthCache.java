package org.example.backend.common.redis;

import lombok.RequiredArgsConstructor;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인증 user 조회 in-memory 캐시 (Redis 미적용 환경 / B5).
 *
 * 목적: Dashboard 페이지 진입 시 5개 endpoint 가 병렬 호출되면서 매번 user 1번씩 fetch 하는
 *       반복 쿼리 제거. TTL 60초 — 권한 변경 시 stale 영향 최소화.
 *
 * 적용 범위: DashboardAggregateService.findUser() 만 사용. JWT 인증 필터의 user 조회는
 *           보안 핵심이므로 건드리지 않음 (별도 영역).
 *
 * 메모리: HashMap entry 1개당 user 객체 + long. 사용자 수가 매우 많아도 60초 TTL 로 자연 만료.
 *
 * Redis 적용 시: 이 클래스는 그대로 두고, Spring Cache + Redis 로 교체 가능 (인터페이스 일치).
 */
@Component
@RequiredArgsConstructor
public class UserAuthCache {

    private static final long TTL_MS = 60_000;  // 60초

    private final UserRepository userRepository;

    private record Entry(User user, long expireAt) {}

    private final Map<Long, Entry> cache = new ConcurrentHashMap<>();

    /**
     * 캐시 우선 조회. miss 면 DB fetch 후 캐시 적재.
     * userIdx null → UNAUTHORIZED 즉시.
     */
    public User loadUser(Long userIdx) {
        if (userIdx == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found.");
        }
        Entry e = cache.get(userIdx);
        long now = System.currentTimeMillis();
        if (e != null && e.expireAt > now) {
            return e.user;
        }
        if (e != null) {
            // 만료된 엔트리 제거
            cache.remove(userIdx);
        }
        User u = userRepository.findById(userIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
        cache.put(userIdx, new Entry(u, now + TTL_MS));
        return u;
    }

    /**
     * 권한/조직 변경 시 호출하여 stale 캐시 제거.
     */
    public void invalidate(Long userIdx) {
        if (userIdx != null) cache.remove(userIdx);
    }

    /** 인증 캐시 전체 초기화 (운영용). */
    public Optional<Integer> clearAll() {
        int n = cache.size();
        cache.clear();
        return Optional.of(n);
    }
}
