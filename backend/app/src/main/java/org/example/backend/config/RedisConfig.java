package org.example.backend.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.api.StatefulConnection;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.example.backend.common.redis.CacheNames;
import org.example.backend.notification.service.NotificationSseService;
import org.example.backend.notification.service.SseMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 캐시 설정.
 * - 캐시별 TTL 차등 적용 (대시보드 5분, 권한 2분, 메타데이터 1시간 등)
 * - JSON 직렬화 (Jackson) — 디버깅 친화적, Redis 에서 사람이 읽을 수 있음
 * - RefreshToken / JWT 블랙리스트 등 String 값은 StringRedisTemplate 사용
 *
 * 캐시 무효화 책임:
 *   - 대시보드:      데이터 변경 시점에 @CacheEvict (KPI/캠페인 변경 메서드)
 *   - 캠페인 권한:   ★ 변경 즉시 evict 필수 (보안 critical)
 *   - 메타데이터:    TTL 만료 위주 + 변경 시 evict
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /** 캐시별 TTL — 도메인 특성에 맞춰 차등. 운영 중 조정 시 여기만 만지면 됨. */
    private static final Map<String, Duration> CACHE_TTL = Map.ofEntries(
            Map.entry(CacheNames.DASHBOARD_PAGE,            Duration.ofMinutes(2)),  // ⚡ Fix A: 통합 응답 — 짧게 (사용자 변경 빨리 반영)
            Map.entry(CacheNames.DASHBOARD_SUMMARY,         Duration.ofMinutes(3)),
            Map.entry(CacheNames.DASHBOARD_QUARTER_GOALS,   Duration.ofMinutes(5)),
            Map.entry(CacheNames.DASHBOARD_PARTNER_PROGRESS,Duration.ofMinutes(3)),
            Map.entry(CacheNames.DASHBOARD_REVIEW_QUEUE,    Duration.ofMinutes(1)),
            Map.entry(CacheNames.DASHBOARD_BLOCKERS,        Duration.ofMinutes(2)),
            Map.entry(CacheNames.DASHBOARD_ASSET_CATEGORIES,Duration.ofMinutes(5)),
            Map.entry(CacheNames.DASHBOARD_KPI_CATEGORIES,  Duration.ofMinutes(5)),

            // Zone 신규 — 실시간성/변경빈도에 따라 차등
            Map.entry(CacheNames.DASHBOARD_RECENT_ACTIVITY, Duration.ofSeconds(30)), // 활동 피드 — 실시간성 ↑
            Map.entry(CacheNames.DASHBOARD_AD_REVIEW_QUEUE, Duration.ofMinutes(1)),  // 검수 큐 — 변경 잦음
            Map.entry(CacheNames.DASHBOARD_CAMPAIGN_PIPELINE,Duration.ofMinutes(3)),
            Map.entry(CacheNames.DASHBOARD_CAMPAIGN_PROGRESS,Duration.ofMinutes(3)),
            Map.entry(CacheNames.DASHBOARD_REVENUE_YOY,     Duration.ofMinutes(10)), // 월별 매출 — 거의 안 변함

            Map.entry(CacheNames.CAMPAIGN_LIST,             Duration.ofMinutes(2)),  // 캠페인 목록 — 변경 잦아 짧게
            Map.entry(CacheNames.CAMPAIGN_MEMBER_ROLE,      Duration.ofMinutes(2)),  // ★ 짧게, evict 보강
            Map.entry(CacheNames.TASK_LIST,                 Duration.ofMinutes(1)),
            Map.entry(CacheNames.CAMPAIGN_KPI,              Duration.ofMinutes(3)),
            Map.entry(CacheNames.KPI_TEMPLATES,             Duration.ofHours(1)),
            Map.entry(CacheNames.NOTIFICATION_SETTING,      Duration.ofMinutes(30)),
            Map.entry(CacheNames.USER_AUTH,                 Duration.ofSeconds(60))   // 기존 in-memory와 동일 TTL

    );

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory(RedisProperties properties) {
        return new LettuceConnectionFactory(
                redisConfiguration(properties),
                lettuceClientConfiguration(properties, null));
    }

    @Bean("cacheRedisConnectionFactory")
    public RedisConnectionFactory cacheRedisConnectionFactory(RedisProperties properties) {
        return new LettuceConnectionFactory(
                redisConfiguration(properties),
                lettuceClientConfiguration(properties, ReadFrom.REPLICA_PREFERRED));
    }

    @Bean
    public RedisCacheManager cacheManager(
            @Qualifier("cacheRedisConnectionFactory") RedisConnectionFactory cf) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // ⚡ 값 직렬화는 JSON → GZIP 압축 (대시보드 ~17KB 응답을 약 70% 축소, 메모리/네트워크 절감)
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GzipRedisSerializer(jsonSerializer())));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        CACHE_TTL.forEach((name, ttl) ->
                perCache.put(name, defaults.entryTtl(ttl))
        );

        return RedisCacheManager.builder(cf)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    /** 직접 키-값 접근용 (RefreshToken, JWT 블랙리스트 등 String 데이터). */
    @Bean
    public StringRedisTemplate stringRedisTemplate(
            @Qualifier("redisConnectionFactory") RedisConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }

    /**
     * 다형성 타입 정보를 포함하는 Jackson 직렬화.
     * - PROPERTY 형식 ({"@class": "...", ...}) — write/read 형식 일관성 보장.
     *   기본 WRAPPER_ARRAY 는 BaseResponse 같은 generic wrapper 에서 deserialize 실패 가능.
     * - LocalDateTime 등 java.time 타입 ISO-8601 문자열로 처리.
     * - ⚡ 보안 강화: 역직렬화 허용 타입을 프로젝트 패키지 + Java 표준 타입으로 제한.
     *   기존 allowIfBaseType(Object.class)는 사실상 제한 없음 → Gadget Chain 공격 위험.
     */
    private GenericJackson2JsonRedisSerializer jsonSerializer() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        // 프로젝트 내부 패키지만 허용
                        .allowIfSubType("org.example.backend.")
                        // Java 표준 컬렉션/자료구조 허용
                        .allowIfSubType(java.util.Collection.class)
                        .allowIfSubType(java.util.Map.class)
                        // 숫자, 문자열, 불리언 등 값 타입
                        .allowIfSubType(java.lang.Number.class)
                        .allowIfSubType(java.lang.String.class)
                        .allowIfSubType(java.lang.Boolean.class)
                        // java.time 타입
                        .allowIfSubType(java.time.temporal.Temporal.class)
                        .allowIfSubType(java.util.Date.class)
                        .build(),
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(om);
    }

    /**
     * Notification Redis에 등록
     */
    @Bean
    public RedisMessageListenerContainer sseListenerContainer(
            @Qualifier("redisConnectionFactory") RedisConnectionFactory cf,
            NotificationSseService sseService,
            ObjectMapper objectMapper) {

        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(cf);

        MessageListener listener = (message, pattern) -> {
            try {
                SseMessage msg = objectMapper.readValue(message.getBody(), SseMessage.class);
                sseService.deliverLocally(msg);           // ⬅ 각 Pod이 자기 연결로 전송
            } catch (Exception ignored) { }
        };

        container.addMessageListener(listener,
                new ChannelTopic(NotificationSseService.SSE_Channel));
        return container;
    }

    private RedisConfiguration redisConfiguration(RedisProperties properties) {
        RedisProperties.Sentinel sentinel = properties.getSentinel();
        if (sentinel != null && hasText(sentinel.getMaster())) {
            return sentinelConfiguration(properties, sentinel);
        }

        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(
                properties.getHost(),
                properties.getPort());
        standalone.setDatabase(properties.getDatabase());
        applyServerAuth(standalone, properties);
        return standalone;
    }

    private RedisSentinelConfiguration sentinelConfiguration(
            RedisProperties properties,
            RedisProperties.Sentinel sentinel) {
        RedisSentinelConfiguration config = new RedisSentinelConfiguration();
        config.master(sentinel.getMaster());
        if (sentinel.getNodes() != null) {
            sentinel.getNodes().forEach(node -> {
                String[] hostAndPort = node.split(":");
                if (hostAndPort.length != 2) {
                    throw new IllegalArgumentException("Invalid Redis sentinel node: " + node);
                }
                config.sentinel(hostAndPort[0].trim(), Integer.parseInt(hostAndPort[1].trim()));
            });
        }
        config.setDatabase(properties.getDatabase());
        applyServerAuth(config, properties);
        if (hasText(sentinel.getPassword())) {
            config.setSentinelPassword(RedisPassword.of(sentinel.getPassword()));
        }
        return config;
    }

    private LettuceClientConfiguration lettuceClientConfiguration(
            RedisProperties properties,
            ReadFrom readFrom) {
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder =
                LettucePoolingClientConfiguration.builder()
                        .poolConfig(poolConfig(properties));

        if (properties.getTimeout() != null) {
            builder.commandTimeout(properties.getTimeout());
        }
        if (properties.getLettuce().getShutdownTimeout() != null) {
            builder.shutdownTimeout(properties.getLettuce().getShutdownTimeout());
        }
        if (readFrom != null) {
            builder.readFrom(readFrom);
        }
        return builder.build();
    }

    private GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig(RedisProperties properties) {
        RedisProperties.Pool pool = properties.getLettuce().getPool();
        GenericObjectPoolConfig<StatefulConnection<?, ?>> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(pool.getMaxActive());
        config.setMaxIdle(pool.getMaxIdle());
        config.setMinIdle(pool.getMinIdle());
        if (pool.getMaxWait() != null) {
            config.setMaxWait(pool.getMaxWait());
        }
        return config;
    }

    private void applyServerAuth(RedisStandaloneConfiguration config, RedisProperties properties) {
        if (hasText(properties.getUsername())) {
            config.setUsername(properties.getUsername());
        }
        if (hasText(properties.getPassword())) {
            config.setPassword(RedisPassword.of(properties.getPassword()));
        }
    }

    private void applyServerAuth(RedisSentinelConfiguration config, RedisProperties properties) {
        if (hasText(properties.getUsername())) {
            config.setUsername(properties.getUsername());
        }
        if (hasText(properties.getPassword())) {
            config.setPassword(RedisPassword.of(properties.getPassword()));
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
