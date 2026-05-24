package org.example.backend.common.redis;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.campaign.repository.CampaignParticipantRepository;
import org.example.backend.user.model.UserAccountStatus;
import org.example.backend.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DashboardCacheEvictor {

    static final Set<String> DASHBOARD_VIEWER_ROLES = Set.of(
            "ROLE_ADMIN",
            "ROLE_GENERAL_MANAGER",
            "ROLE_MANAGER"
    );

    private final DashboardCacheVersionService dashboardCacheVersionService;
    private final CampaignMemberRepository campaignMemberRepository;
    private final CampaignParticipantRepository campaignParticipantRepository;
    private final UserRepository userRepository;

    @Caching(evict = {
            @CacheEvict(value = CacheNames.CAMPAIGN_LIST,              allEntries = true)
    })
    public void evictAll() {
        dashboardCacheVersionService.increaseGlobalVersion();
    }

    @Caching(evict = {
            @CacheEvict(value = CacheNames.CAMPAIGN_LIST,              allEntries = true)
    })
    public void evictUser(Long userIdx) {
        if (userIdx == null) {
            return;
        }

        dashboardCacheVersionService.increaseUserVersion(userIdx);
    }

    @Caching(evict = {
            @CacheEvict(value = CacheNames.CAMPAIGN_LIST,              allEntries = true)
    })
    public void evictUsers(Collection<Long> userIdxs) {
        if (userIdxs == null || userIdxs.isEmpty()) {
            return;
        }

        userIdxs.stream()
                .filter(userIdx -> userIdx != null)
                .distinct()
                .forEach(dashboardCacheVersionService::increaseUserVersion);
    }

    @Caching(evict = {
            @CacheEvict(value = CacheNames.CAMPAIGN_LIST,              allEntries = true)
    })
    public void evictCampaign(Long campaignIdx) {
        if (campaignIdx == null) {
            return;
        }

        evictUsers(resolveCampaignDashboardUsers(campaignIdx));
    }

    @Caching(evict = {
            @CacheEvict(value = CacheNames.CAMPAIGN_LIST,              allEntries = true)
    })
    public void evictCampaigns(Collection<Long> campaignIdxs) {
        if (campaignIdxs == null || campaignIdxs.isEmpty()) {
            return;
        }

        Set<Long> affectedUserIdxs = new LinkedHashSet<>();
        campaignIdxs.stream()
                .filter(campaignIdx -> campaignIdx != null)
                .distinct()
                .map(this::resolveCampaignDashboardUsers)
                .forEach(affectedUserIdxs::addAll);

        evictUsers(affectedUserIdxs);
    }

    private Set<Long> resolveCampaignDashboardUsers(Long campaignIdx) {
        Set<Long> affectedUserIdxs = new LinkedHashSet<>();
        affectedUserIdxs.addAll(campaignMemberRepository.findUserIdxByCampaignIdx(campaignIdx));

        Set<Long> organizationIdxs = campaignParticipantRepository.findOrganizationIdxByCampaignIdx(campaignIdx);
        if (!organizationIdxs.isEmpty()) {
            affectedUserIdxs.addAll(userRepository.findUserIdxByOrganizationIdxInAndRoleInAndAccountStatus(
                    organizationIdxs,
                    DASHBOARD_VIEWER_ROLES,
                    UserAccountStatus.ACTIVE
            ));
        }

        return affectedUserIdxs;
    }
}
