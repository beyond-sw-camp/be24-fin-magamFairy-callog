package org.example.backend.user.service;

import org.example.backend.support.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RefreshToken Redis 이전 검증.
 *
 * 검증 시나리오:
 *   1. 저장 → 조회 (정방향, 역방향)
 *   2. 같은 userId 재저장 → 옛 토큰 자동 삭제 (로테이션)
 *   3. 사용자 로그아웃 → 양방향 인덱스 모두 삭제
 *   4. 특정 토큰만 무효화 (다중 디바이스 시나리오)
 *
 * TTL 만료 검증은 시간 소요로 별도 (Awaitility 또는 짧은 TTL 로 검증 가능).
 */
@SpringBootTest(classes = {
        org.example.backend.config.RedisConfig.class,
        RefreshTokenRedisService.class
})
@ContextConfiguration(initializers = AbstractRedisIntegrationTest.RedisContextInitializer.class)
class RefreshTokenRedisServiceTest extends AbstractRedisIntegrationTest {

    @Autowired RefreshTokenRedisService service;
    @Autowired StringRedisTemplate redis;

    @BeforeEach
    void cleanRedis() {
        // 매 테스트 격리 — 키 전부 삭제
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("토큰 저장 후 userId 로 조회 가능")
    void save_and_findByUserId() {
        // when
        service.save("user-1", "token-abc");

        // then
        Optional<String> found = service.findByUserId("user-1");
        assertThat(found).contains("token-abc");
    }

    @Test
    @DisplayName("토큰 저장 후 token 으로 userId 역조회 가능")
    void save_and_findUserIdByToken() {
        // when
        service.save("user-1", "token-abc");

        // then
        Optional<String> userId = service.findUserIdByToken("token-abc");
        assertThat(userId).contains("user-1");
    }

    @Test
    @DisplayName("같은 userId 로 재저장 시 옛 토큰의 역인덱스가 자동 삭제됨 (로테이션)")
    void resave_rotates_old_token() {
        // given - 옛 토큰
        service.save("user-1", "old-token");
        assertThat(service.findUserIdByToken("old-token")).contains("user-1");

        // when - 새 토큰으로 갱신
        service.save("user-1", "new-token");

        // then - 새 토큰만 유효, 옛 토큰의 역인덱스는 사라짐
        assertThat(service.findByUserId("user-1")).contains("new-token");
        assertThat(service.findUserIdByToken("new-token")).contains("user-1");
        assertThat(service.findUserIdByToken("old-token")).isEmpty();   // ★ stale 토큰 차단
    }

    @Test
    @DisplayName("사용자 로그아웃 시 양방향 인덱스 모두 삭제")
    void deleteByUserId_clears_both_indices() {
        // given
        service.save("user-1", "token-abc");

        // when
        service.deleteByUserId("user-1");

        // then - userId 조회도, token 역조회도 모두 빈 결과
        assertThat(service.findByUserId("user-1")).isEmpty();
        assertThat(service.findUserIdByToken("token-abc")).isEmpty();
    }

    @Test
    @DisplayName("특정 토큰만 무효화하면 그 토큰만 사라지고 user 인덱스도 정리됨")
    void deleteByToken_clears_specific_token_only() {
        // given
        service.save("user-1", "token-abc");

        // when - 이 토큰만 무효화 (다른 디바이스 로그아웃 시나리오)
        service.deleteByToken("token-abc");

        // then - 양쪽 다 삭제 (현재 active 토큰이 이거였으므로)
        assertThat(service.findUserIdByToken("token-abc")).isEmpty();
        assertThat(service.findByUserId("user-1")).isEmpty();
    }

    @Test
    @DisplayName("다른 사용자 토큰들은 서로 격리됨")
    void tokens_of_different_users_are_isolated() {
        // when
        service.save("user-1", "token-1");
        service.save("user-2", "token-2");

        // then
        assertThat(service.findByUserId("user-1")).contains("token-1");
        assertThat(service.findByUserId("user-2")).contains("token-2");

        // user-1 로그아웃해도 user-2 는 영향 없음
        service.deleteByUserId("user-1");
        assertThat(service.findByUserId("user-1")).isEmpty();
        assertThat(service.findByUserId("user-2")).contains("token-2");
    }
}
