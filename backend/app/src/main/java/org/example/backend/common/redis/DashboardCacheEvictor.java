package org.example.backend.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DashboardCacheEvictor {

    private final DashboardCacheVersionService dashboardCacheVersionService;

    @Caching(evict = {
            @CacheEvict(value = CacheNames.DASHBOARD_SUMMARY,          allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD_QUARTER_GOALS,    allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD_PARTNER_PROGRESS, allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD_REVIEW_QUEUE,     allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD_BLOCKERS,         allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD_ASSET_CATEGORIES, allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD_KPI_CATEGORIES,   allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD_RECENT_ACTIVITY,  allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD_AD_REVIEW_QUEUE,  allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD_CAMPAIGN_PIPELINE,allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD_CAMPAIGN_PROGRESS,allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD_REVENUE_YOY,      allEntries = true),
            @CacheEvict(value = CacheNames.CAMPAIGN_LIST,              allEntries = true)
    })
    public void evictAll() {
        dashboardCacheVersionService.increaseGlobalVersion();
    }
}
