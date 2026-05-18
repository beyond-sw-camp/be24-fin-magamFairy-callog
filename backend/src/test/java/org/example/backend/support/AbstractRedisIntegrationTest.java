package org.example.backend.support;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Redis 통합 테스트 공통 베이스.
 *
 * - Docker 로 진짜 Redis 7 컨테이너를 띄움 (모든 테스트가 공유 = 빠름)
 * - @TestPropertySource 로 Spring 의 Redis 설정을 컨테이너 주소로 동적 주입
 * - 통합 테스트 클래스에서 이 클래스를 상속하고 @SpringBootTest 만 붙이면 됨
 *
 * 사용:
 *   @SpringBootTest
 *   class MyRedisTest extends AbstractRedisIntegrationTest { ... }
 *
 * Docker 가 없으면 테스트가 스킵되니, CI/로컬 모두에서 Docker 필요.
 */
public abstract class AbstractRedisIntegrationTest {

    /** static — 모든 테스트가 같은 컨테이너 공유. JVM 종료 시 자동 정리. */
    protected static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    static {
        REDIS.start();
    }

    /**
     * Spring 컨텍스트가 만들어지기 전에 spring.data.redis.* 프로퍼티를 컨테이너로 향하게.
     * 통합 테스트 클래스에 다음을 추가:
     *   @ContextConfiguration(initializers = RedisContextInitializer.class)
     */
    public static class RedisContextInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            TestPropertyValues.of(
                    "spring.data.redis.host=" + REDIS.getHost(),
                    "spring.data.redis.port=" + REDIS.getMappedPort(6379),
                    // 통합 테스트에선 Sentinel 안 씀 — 단일 노드로 충분
                    "spring.data.redis.sentinel.nodes="
            ).applyTo(ctx.getEnvironment());
        }
    }
}
