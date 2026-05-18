package org.example.backend.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.backend.common.redis.CacheNames;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private static final Map<String, Duration> CACHE_TTL = Map.of(
            CacheNames.DASHBOARD_SUMMARY,         Duration.ofMinutes(3),
            CacheNames.DASHBOARD_QUARTER_GOALS,   Duration.ofMinutes(5),
            CacheNames.DASHBOARD_PARTNER_PROGRESS,Duration.ofMinutes(3),
            CacheNames.DASHBOARD_REVIEW_QUEUE,    Duration.ofMinutes(1),
            CacheNames.DASHBOARD_BLOCKERS,        Duration.ofMinutes(2),
            CacheNames.DASHBOARD_ASSET_CATEGORIES,Duration.ofMinutes(5),
            CacheNames.DASHBOARD_KPI_CATEGORIES,  Duration.ofMinutes(5),

            CacheNames.CAMPAIGN_MEMBER_ROLE,      Duration.ofMinutes(2),  // ★ 짧게, evict 보강
            CacheNames.KPI_TEMPLATES,             Duration.ofHours(1),
            CacheNames.NOTIFICATION_SETTING,      Duration.ofMinutes(30)
    );

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer()));

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
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }

    /**
     * 다형성 타입 정보를 포함하는 Jackson 직렬화.
     * - PROPERTY 형식 ({"@class": "...", ...}) — write/read 형식 일관성 보장.
     *   기본 WRAPPER_ARRAY 는 BaseResponse 같은 generic wrapper 에서 deserialize 실패 가능.
     * - LocalDateTime 등 java.time 타입 ISO-8601 문자열로 처리.
     */
    private GenericJackson2JsonRedisSerializer jsonSerializer() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(om);
    }
}
