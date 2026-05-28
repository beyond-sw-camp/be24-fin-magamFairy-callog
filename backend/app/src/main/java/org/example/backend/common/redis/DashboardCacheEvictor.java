package org.example.backend.common.redis;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.campaign.repository.CampaignParticipantRepository;
import org.example.backend.user.model.UserAccountStatus;
import org.example.backend.user.repository.UserRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DashboardCacheEvictor {

    static final Set<String> DASHBOARD_VIEWER_ROLES = Set.of(
            "ROLE_ADMIN",
            "ROLE_GENERAL_MANAGER",
            "ROLE_MANAGER"
    );
    private static final List<String> CAMPAIGN_LIST_SCOPES = List.of("mine", "org");

    private final DashboardCacheVersionService dashboardCacheVersionService;
    private final CampaignMemberRepository campaignMemberRepository;
    private final CampaignParticipantRepository campaignParticipantRepository;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    public void evictAll() {
        dashboardCacheVersionService.increaseGlobalVersion();
    }

    public void evictUser(Long userIdx) {
        if (userIdx == null) {
            return;
        }

        dashboardCacheVersionService.increaseUserVersion(userIdx);
        evictCampaignList(userIdx);
    }

    public void evictUsers(Collection<Long> userIdxs) {
        if (userIdxs == null || userIdxs.isEmpty()) {
            return;
        }

        Set<Long> distinctUserIdxs = userIdxs.stream()
                .filter(userIdx -> userIdx != null)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        distinctUserIdxs.forEach(userIdx -> {
            dashboardCacheVersionService.increaseUserVersion(userIdx);
            evictCampaignList(userIdx);
        });
    }

    public void evictCampaign(Long campaignIdx) {
        if (campaignIdx == null) {
            return;
        }

        evictUsers(resolveCampaignDashboardUsers(campaignIdx));
    }

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

    private void evictCampaignList(Long userIdx) {
        Cache cache = cacheManager.getCache(CacheNames.CAMPAIGN_LIST);
        if (cache == null) {
            return;
        }

        CAMPAIGN_LIST_SCOPES.forEach(scope -> cache.evict(userIdx + ":" + scope));
    }
}
