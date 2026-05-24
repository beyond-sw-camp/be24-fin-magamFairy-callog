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
            @CacheEvict(value = CacheNames.CAMPAIGN_LIST,              allEntries = true)
    })
    public void evictAll() {
        dashboardCacheVersionService.increaseGlobalVersion();
    }
}
