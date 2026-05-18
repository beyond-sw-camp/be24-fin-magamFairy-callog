package org.example.backend.campaign.service;

import org.example.backend.common.redis.CacheNames;
import org.example.backend.support.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 캠페인 멤버 권한 캐시 검증 — ★ 보안 critical 회귀 테스트
 *
 * ⚠️ 현재 비활성화(@Disabled).
 *    이 테스트는 단계 4 (CampaignMemberService 권한 캐시 적용) 완료 후 활성화한다.
 *
 * 활성화 조건 (단계 4 적용):
 *   1. CampaignMemberService 에 권한 조회 메서드 신설:
 *        @Cacheable(value = CacheNames.CAMPAIGN_MEMBER_ROLE,
 *                   key = "#campaignId + ':' + #userIdx")
 *        public CampaignMemberRole getRole(Long campaignId, Long userIdx)
 *
 *   2. 권한 변경 메서드들에 evict 추가:
 *        - updateMemberRole(...) 끝에 cacheManager.getCache(...).evict(...)
 *        - removeMember(...) 끝에 같은 처리
 *      또는 @CacheEvict(value = CacheNames.CAMPAIGN_MEMBER_ROLE, allEntries = true)
 *
 *   3. 아래 주석 처리된 테스트 본문을 살리고 @Disabled 제거
 *
 * 권한 캐시는 stale = 보안 사고이므로, 이 테스트는 권한 코드 건드릴 때마다 반드시 통과해야 함.
 */
@Disabled("단계 4 권한 캐시 적용 후 활성화 — CampaignMemberService 에 getRole 신설 + evict 적용 필요")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(initializers = AbstractRedisIntegrationTest.RedisContextInitializer.class)
class CampaignMemberPermissionCacheTest extends AbstractRedisIntegrationTest {

    @Autowired CacheManager cacheManager;
    @Autowired CampaignMemberService service;

    @BeforeEach
    void clearCache() {
        cacheManager.getCache(CacheNames.CAMPAIGN_MEMBER_ROLE).clear();
    }

    @Test
    @DisplayName("권한 조회 후 캐시에 적재됨")
    void getRole_caches_result() {
        // 단계 4 적용 후 활성화:
        // service.getRole(100L, 200L);
        // assertThat(cacheManager.getCache(CacheNames.CAMPAIGN_MEMBER_ROLE).get("100:200")).isNotNull();
    }

    @Test
    @DisplayName("★ updateMemberRole 호출 시 그 사용자의 권한 캐시가 즉시 evict (stale 차단)")
    void updateRole_evicts_cache_immediately() {
        // 단계 4 적용 후 활성화:
        // service.getRole(100L, 200L);
        // String cacheKey = "100:200";
        // assertThat(cacheManager.getCache(CacheNames.CAMPAIGN_MEMBER_ROLE).get(cacheKey)).isNotNull();
        //
        // service.updateMemberRole(100L, "caller", 200L, CampaignMemberRole.USER);
        //
        // assertThat(cacheManager.getCache(CacheNames.CAMPAIGN_MEMBER_ROLE).get(cacheKey))
        //         .as("권한 변경 직후 캐시 evict 필수 — stale 시 보안 사고")
        //         .isNull();
    }

    @Test
    @DisplayName("★ removeMember 호출 시 그 사용자의 권한 캐시가 즉시 evict (추방 보안)")
    void removeMember_evicts_cache_immediately() {
        // 단계 4 적용 후 활성화:
        // service.getRole(100L, 200L);
        // service.removeMember(100L, "caller", 200L);
        // assertThat(cacheManager.getCache(CacheNames.CAMPAIGN_MEMBER_ROLE).get("100:200")).isNull();
    }

    @Test
    @DisplayName("캠페인별 / 사용자별 키 분리 — 한 캠페인 변경이 다른 캠페인 캐시에 영향 없음")
    void cache_keys_are_isolated_per_campaign_and_user() {
        // 단계 4 적용 후 활성화
    }

    @Test
    @DisplayName("evict 후 재조회 시 DB 다시 호출 (캐시 미적재 → 재적재)")
    void after_evict_next_call_fetches_from_db_again() {
        // 단계 4 적용 후 활성화
    }
}
