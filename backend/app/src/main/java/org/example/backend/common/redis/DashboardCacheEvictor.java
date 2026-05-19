package org.example.backend.common.redis;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;


/**
 *  Dashboard 관련 8개의 캐시들을  한번에 evict 하는 헬퍼 빈
 *  AOP 프록시 기반이므로 같은 클래스 내부에서 호출하지 말 것
 */

@Component
public class DashboardCacheEvictor {

    // 얘네는 대시보드의 8개를 전체 초기화를 시키는 메서드

    @Caching(evict = {
            @CacheEvict(value = CacheNames.DASHBOARD_PAGE,             allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD_SUMMARY,          allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD_QUARTER_GOALS,    allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD_PARTNER_PROGRESS, allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD_REVIEW_QUEUE,     allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD_BLOCKERS,         allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD_ASSET_CATEGORIES, allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD_KPI_CATEGORIES,   allEntries = true)
    })
    public void evictAll() {
        // 메서드 본문 없음 — Spring AOP 가 @Caching 어노테이션을 보고 evict 수행
        // 메서드 호출하면 위의 내용들을 전체 초기화
    }
}
