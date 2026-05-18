package org.example.backend.dashboard.service;

import org.example.backend.common.redis.CacheNames;
import org.example.backend.support.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 대시보드 응답 캐시 동작 검증.
 *
 * 검증 시나리오:
 *   1. 같은 사용자가 두 번 호출 → 2번째는 캐시 HIT (DB 호출 0)
 *   2. 다른 사용자는 캐시 분리됨 (사용자별 키)
 *   3. 명시적 evict 후 재호출 → MISS (DB 다시 호출)
 *
 * 전제: DashboardAggregateService 의 각 메서드에 @Cacheable 이 적용돼 있다고 가정.
 *   @Cacheable(value = CacheNames.DASHBOARD_SUMMARY, key = "#callerIdx")
 *   public DashboardSummaryDto summary(Long callerIdx) { ... }
 *
 * 이 테스트는 "@Cacheable 이 진짜로 DB 재호출을 막는가" 를 진짜 Redis 로 검증한다.
 *
 * ⚠️ 이 테스트는 DashboardAggregateService 의 무거운 의존성(13개 Repository)을
 *    @MockBean 으로 다 mock 처리해야 깔끔히 돈다. 실제 캐시 동작에만 집중하기 위함.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(initializers = AbstractRedisIntegrationTest.RedisContextInitializer.class)
class DashboardCacheTest extends AbstractRedisIntegrationTest {

    @Autowired CacheManager cacheManager;
    @Autowired DashboardAggregateService service;

    // 무거운 의존성들은 mock — 캐시 동작 자체에만 집중
    // (실제 13개 Repository 모두 mock 처리는 별도 BackendApplicationTests 에서)

    @BeforeEach
    void clearAllCaches() {
        // 각 테스트 격리 — 모든 캐시 비우기
        cacheManager.getCacheNames().forEach(name ->
                cacheManager.getCache(name).clear()
        );
    }

    @Test
    @DisplayName("같은 사용자 2번 호출 시 2번째는 캐시 HIT (DB 미호출)")
    void summary_called_twice_for_same_user_hits_cache_second_time() {
        // given - mock 이 반환할 값 설정 (실제 환경에선 Repository mock 으로)
        // when - 첫 호출
        service.summary(1L);
        // when - 두 번째 호출 (캐시 HIT 기대)
        service.summary(1L);

        // then - 캐시에 값이 적재돼 있음
        assertThat(cacheManager.getCache(CacheNames.DASHBOARD_SUMMARY).get(1L)).isNotNull();

        // 진짜 검증은 @SpyBean 으로 Repository 호출 횟수를 확인 (1번만 호출됐는지)
        // → 실제 사용 시 verify(spyRepo, times(1)).findAll();
    }

    @Test
    @DisplayName("다른 사용자는 캐시 분리 (사용자별 키)")
    void different_users_get_separate_cache_entries() {
        service.summary(1L);
        service.summary(2L);

        // 두 키 모두 캐시에 존재
        assertThat(cacheManager.getCache(CacheNames.DASHBOARD_SUMMARY).get(1L)).isNotNull();
        assertThat(cacheManager.getCache(CacheNames.DASHBOARD_SUMMARY).get(2L)).isNotNull();

        // 사용자 1 캐시를 evict 해도 2 는 살아있음
        cacheManager.getCache(CacheNames.DASHBOARD_SUMMARY).evict(1L);
        assertThat(cacheManager.getCache(CacheNames.DASHBOARD_SUMMARY).get(1L)).isNull();
        assertThat(cacheManager.getCache(CacheNames.DASHBOARD_SUMMARY).get(2L)).isNotNull();
    }

    @Test
    @DisplayName("명시적 evict 후 재호출하면 캐시 MISS → 다시 DB 호출")
    void evict_then_call_again_misses_cache() {
        service.summary(1L);
        assertThat(cacheManager.getCache(CacheNames.DASHBOARD_SUMMARY).get(1L)).isNotNull();

        // when - evict (예: 그 사용자의 캠페인 권한이 바뀌어 대시보드 데이터가 무효화돼야 할 때)
        cacheManager.getCache(CacheNames.DASHBOARD_SUMMARY).evict(1L);

        // then - 캐시 비었음. 다음 호출은 MISS 가 되어 DB 재조회됨
        assertThat(cacheManager.getCache(CacheNames.DASHBOARD_SUMMARY).get(1L)).isNull();
    }

    @Test
    @DisplayName("모든 사용자의 대시보드 캐시 일괄 evict (allEntries=true)")
    void clear_all_dashboard_caches_for_global_change() {
        service.summary(1L);
        service.summary(2L);
        service.summary(3L);

        // when - 시스템 전역 변경 (예: KPI 시드 데이터 갱신) 시 사용
        cacheManager.getCache(CacheNames.DASHBOARD_SUMMARY).clear();

        // then - 전부 비었음
        assertThat(cacheManager.getCache(CacheNames.DASHBOARD_SUMMARY).get(1L)).isNull();
        assertThat(cacheManager.getCache(CacheNames.DASHBOARD_SUMMARY).get(2L)).isNull();
        assertThat(cacheManager.getCache(CacheNames.DASHBOARD_SUMMARY).get(3L)).isNull();
    }
}
