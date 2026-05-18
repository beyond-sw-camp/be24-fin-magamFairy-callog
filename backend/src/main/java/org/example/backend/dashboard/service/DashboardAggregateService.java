package org.example.backend.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignKpi;
import org.example.backend.campaign.model.CampaignMember;
import org.example.backend.campaign.model.CampaignMemberRole;
import org.example.backend.campaign.model.CampaignParticipant;
import org.example.backend.campaign.repository.CampaignKpiRepository;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.campaign.repository.CampaignParticipantRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.dashboard.dto.BlockerDto;
import org.example.backend.dashboard.dto.DashboardSummaryDto;
import org.example.backend.dashboard.dto.PartnerProgressDto;
import org.example.backend.dashboard.dto.QuarterGoalProgressDto;
import org.example.backend.dashboard.dto.ReviewQueueItemDto;
import org.example.backend.kpi.model.GoalStatus;
import org.example.backend.kpi.model.KpiDailySnapshot;
import org.example.backend.kpi.model.KpiMonthlySnapshot;
import org.example.backend.kpi.model.OrganizationKpi;
import org.example.backend.kpi.repository.CampaignKpiContributionRepository;
import org.example.backend.kpi.repository.KpiDailySnapshotRepository;
import org.example.backend.kpi.repository.KpiMonthlySnapshotRepository;
import org.example.backend.kpi.repository.OrganizationKpiRepository;
import org.example.backend.matching.repository.AssetRepository;
import org.example.backend.matching.repository.BenefitRepository;
import org.example.backend.organization.model.Organization;
import org.example.backend.organization.model.OrganizationType;
import org.example.backend.organization.repository.OrganizationRepository;
import org.example.backend.teamboard.model.Task;
import org.example.backend.teamboard.model.TaskStatus;
import org.example.backend.teamboard.repository.TaskRepository;
import org.example.backend.common.redis.CacheNames;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardAggregateService {

    private static final Set<String> ACTIVE_CAMPAIGN_STATUSES = Set.of("live", "review", "paused");

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignKpiRepository campaignKpiRepository;
    private final CampaignMemberRepository memberRepository;
    private final CampaignParticipantRepository participantRepository;
    private final OrganizationKpiRepository orgKpiRepository;
    private final CampaignKpiContributionRepository contributionRepository;
    private final TaskRepository taskRepository;
    private final AssetRepository assetRepository;
    private final BenefitRepository benefitRepository;
    private final KpiMonthlySnapshotRepository monthlySnapshotRepository;
    private final KpiDailySnapshotRepository dailySnapshotRepository;

    // ── 1. Summary ──────────────────────────────────────────

    @Cacheable(value = CacheNames.DASHBOARD_SUMMARY, key = "#callerIdx")
    public DashboardSummaryDto summary(Long callerIdx) {
        User caller = findUser(callerIdx);
        Scope scope = resolveScope(caller);
        List<Campaign> visibleCampaigns = filterCampaigns(scope);

        long active = visibleCampaigns.stream()
                .filter(c -> ACTIVE_CAMPAIGN_STATUSES.contains(c.getStatus()))
                .count();

        Set<Long> visibleCampaignIds = visibleCampaigns.stream()
                .map(Campaign::getIdx).collect(Collectors.toSet());
        List<CampaignKpi> kpis = visibleCampaignIds.isEmpty()
                ? List.of()
                : campaignKpiRepository.findAll().stream()
                    .filter(k -> visibleCampaignIds.contains(k.getCampaign().getIdx()))
                    .toList();
        Integer avg = averageAchievement(kpis);

        // 검수 대기 / 패스율 (REVIEW vs DONE 비율)
        List<Task> scopedTasks = filterTasksForScope(scope);
        long pending = scopedTasks.stream().filter(t -> t.getStatus() == TaskStatus.REVIEW).count();
        long doneCnt = scopedTasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        long reviewedTotal = pending + doneCnt;
        Integer passPct = reviewedTotal == 0 ? null
                : (int) Math.round(doneCnt * 100.0 / reviewedTotal);

        // 협력사 수 (참여 organization)
        Set<Long> orgIds = new HashSet<>();
        for (Campaign c : visibleCampaigns) {
            for (CampaignParticipant cp : participantRepository.findAllByCampaignIdx(c.getIdx())) {
                if (cp.getOrganization() != null) {
                    orgIds.add(cp.getOrganization().getIdx());
                }
            }
        }
        long partnerCount = orgIds.size();

        // 자산 LIVE 카운트 (visible 캠페인 범위)
        long liveAssetCount = assetRepository.findAll().stream()
                .filter(a -> {
                    if (scope.allCampaigns) return true;
                    Long camp = a.getCampaign() != null ? a.getCampaign().getIdx() : null;
                    Long org = a.getOrganization() != null ? a.getOrganization().getIdx() : null;
                    return (camp != null && visibleCampaignIds.contains(camp))
                            || (scope.ownerOrgId != null && Objects.equals(scope.ownerOrgId, org));
                })
                .count();

        // 매칭 평균 — KPI 평균 달성률을 차용 (별도 매칭 점수 데이터 없을 때)
        Integer matchAvg = avg;

        // RFP 응모 = PartnerBenefits 전체 카운트 (Phase 2: visible 캠페인 범위로 좁히기)
        long rfpCount = benefitRepository.count();

        // 신규 협력사 — visible org 중 createdAt가 30일 내인 것
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long newPartnerCount = orgIds.isEmpty() ? 0L
                : organizationRepository.findAllById(orgIds).stream()
                        .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(thirtyDaysAgo))
                        .count();

        List<DashboardSummaryDto.MiniStat> miniStats = List.of(
                new DashboardSummaryDto.MiniStat("검수 패스율", passPct == null ? "-" : passPct + "%"),
                new DashboardSummaryDto.MiniStat("매칭 평균", matchAvg == null ? "-" : matchAvg + "점"),
                new DashboardSummaryDto.MiniStat("자산 LIVE", String.valueOf(liveAssetCount))
        );

        // AFFILIATE / EXTERNAL_PARTNER 스코프: 전사 OrgKpi 평균 — 자기 위치 비교용
        Integer companyAvgPct = null;
        if ("AFFILIATE".equals(scope.label) || "EXTERNAL_PARTNER".equals(scope.label)) {
            List<OrganizationKpi> allActive = orgKpiRepository.findByFilters(null, null, GoalStatus.ACTIVE);
            List<Integer> percents = allActive.stream()
                    .map(k -> calcPercent(
                            contributionRepository.sumActualByTargetOrgKpi(k.getIdx()),
                            k.getTargetValue()))
                    .filter(Objects::nonNull)
                    .toList();
            companyAvgPct = percents.isEmpty() ? null
                    : percents.stream().mapToInt(Integer::intValue).sum() / percents.size();
        }

        return new DashboardSummaryDto(
                active, avg, avg,
                pending, partnerCount, newPartnerCount, rfpCount,
                0, companyAvgPct, miniStats, scope.label
        );
    }

    // ── 2. Quarter Goals ────────────────────────────────────

    @Cacheable(value = CacheNames.DASHBOARD_QUARTER_GOALS,
               key = "#callerIdx + ':' + (#periodCode ?: 'all')")
    public List<QuarterGoalProgressDto> quarterGoals(Long callerIdx, String periodCode) {
        User caller = findUser(callerIdx);
        Scope scope = resolveScope(caller);

        // ACTIVE 상태 OrganizationKpi 중 period 일치 + 권한 분기
        List<OrganizationKpi> kpis = orgKpiRepository.findByFilters(
                resolveOwnerOrgIdFilter(scope), nullIfBlank(periodCode), GoalStatus.ACTIVE);

        return kpis.stream().map(this::toQuarterGoalDto).toList();
    }

    private QuarterGoalProgressDto toQuarterGoalDto(OrganizationKpi k) {
        BigDecimal committed = contributionRepository.sumCommittedByTargetOrgKpi(k.getIdx());
        BigDecimal actual = contributionRepository.sumActualByTargetOrgKpi(k.getIdx());
        Integer pct = calcPercent(actual, k.getTargetValue());

        // 월별 데이터 — snapshot 없는 달은 null (frontend 우측 정렬에 사용)
        List<Integer> monthlyActuals = new ArrayList<>(3);
        List<Integer> monthlyTargets = new ArrayList<>(3);
        int[] qMonths = parseQuarterMonths(k.getPeriodCode());
        if (qMonths != null) {
            int year = qMonths[0];
            List<KpiMonthlySnapshot> snaps = monthlySnapshotRepository
                    .findAllByOrgKpi_IdxAndYearOrderByMonthAsc(k.getIdx(), year);
            Map<Integer, KpiMonthlySnapshot> byMonth = snaps.stream()
                    .collect(Collectors.toMap(KpiMonthlySnapshot::getMonth, s -> s, (a, b) -> a));
            int currentMonth = LocalDate.now().getMonthValue();
            int currentYear = LocalDate.now().getYear();
            for (int i = 1; i <= 3; i++) {
                int m = qMonths[i];
                KpiMonthlySnapshot s = byMonth.get(m);
                if (s != null) {
                    monthlyActuals.add(s.getActualValue() != null ? s.getActualValue().intValue() : null);
                    monthlyTargets.add(s.getTargetValue() != null ? s.getTargetValue().intValue() : null);
                } else if (year == currentYear && m == currentMonth) {
                    // 현재 진행 중인 달은 snapshot 없어도 누적 actual/target 표시
                    monthlyActuals.add(actual != null ? actual.intValue() : 0);
                    monthlyTargets.add(k.getTargetValue() != null ? k.getTargetValue().intValue() : null);
                } else {
                    monthlyActuals.add(null);
                    monthlyTargets.add(null);
                }
            }
        } else {
            monthlyActuals = List.of();
            monthlyTargets = List.of();
        }

        return new QuarterGoalProgressDto(
                k.getIdx(),
                k.getName(),
                k.getUnit(),
                k.getTargetValue(),
                committed,
                actual,
                pct,
                k.getPeriodCode(),
                k.getOwner() != null ? k.getOwner().getIdx() : null,
                k.getOwner() != null ? k.getOwner().getName() : null,
                k.getCategory() != null ? k.getCategory().name() : null,
                k.getEsgCategory() != null ? k.getEsgCategory().name() : null,
                monthlyActuals,
                monthlyTargets
        );
    }

    /** "2026-Q2" → [2026, 4, 5, 6]. 형식 불일치 시 null. */
    private static int[] parseQuarterMonths(String periodCode) {
        if (periodCode == null) return null;
        try {
            String[] parts = periodCode.split("-Q");
            if (parts.length != 2) return null;
            int year = Integer.parseInt(parts[0].trim());
            int q = Integer.parseInt(parts[1].trim());
            if (q < 1 || q > 4) return null;
            int firstMonth = (q - 1) * 3 + 1;
            return new int[]{year, firstMonth, firstMonth + 1, firstMonth + 2};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ── 3. Partner Progress ─────────────────────────────────

    @Cacheable(value = CacheNames.DASHBOARD_PARTNER_PROGRESS, key = "#callerIdx")
    public List<PartnerProgressDto> partnerProgress(Long callerIdx) {
        User caller = findUser(callerIdx);
        Scope scope = resolveScope(caller);
        List<Campaign> visibleCampaigns = filterCampaigns(scope);

        // 캠페인별 참여 조직을 모음 — partner 후보
        Map<Long, Organization> partners = new HashMap<>();
        Map<Long, Long> totalByOrg = new HashMap<>();
        Map<Long, Long> activeByOrg = new HashMap<>();
        Map<Long, List<CampaignKpi>> kpisByOrg = new HashMap<>();

        for (Campaign c : visibleCampaigns) {
            List<CampaignParticipant> ps = participantRepository.findAllByCampaignIdx(c.getIdx());
            List<CampaignKpi> ckpis = campaignKpiRepository.findAllByCampaignIdxOrderByIdxAsc(c.getIdx());

            for (CampaignParticipant cp : ps) {
                Organization org = cp.getOrganization();
                if (org == null) continue;
                partners.putIfAbsent(org.getIdx(), org);
                totalByOrg.merge(org.getIdx(), 1L, Long::sum);
                if (ACTIVE_CAMPAIGN_STATUSES.contains(c.getStatus())) {
                    activeByOrg.merge(org.getIdx(), 1L, Long::sum);
                }
                kpisByOrg.computeIfAbsent(org.getIdx(), k -> new java.util.ArrayList<>()).addAll(ckpis);
            }
        }

        LocalDate sevenDaysAgo = LocalDate.now().minusDays(6);
        return partners.values().stream()
                .map(org -> {
                    Integer avgPct = averageAchievement(kpisByOrg.getOrDefault(org.getIdx(), List.of()));
                    return new PartnerProgressDto(
                            org.getIdx(),
                            org.getName(),
                            totalByOrg.getOrDefault(org.getIdx(), 0L),
                            activeByOrg.getOrDefault(org.getIdx(), 0L),
                            avgPct,
                            recent7dFromSnapshots(org.getIdx(), sevenDaysAgo, avgPct)
                    );
                })
                .toList();
    }

    /** 최근 7일 daily snapshot 조회. 부족하면 평균 점수 기반 stub로 채움. */
    private List<Integer> recent7dFromSnapshots(Long orgId, LocalDate from, Integer fallbackAvg) {
        List<KpiDailySnapshot> snaps = dailySnapshotRepository
                .findAllByOrganization_IdxAndDateGreaterThanEqualOrderByDateAsc(orgId, from);
        if (snaps.size() >= 7) {
            return snaps.stream()
                    .skip(Math.max(0, snaps.size() - 7))
                    .map(s -> s.getAvgKpiPercent() == null ? 0 : s.getAvgKpiPercent())
                    .toList();
        }
        // 충분한 daily snapshot 누적 전: 기존 stub 유지
        if (fallbackAvg == null) return List.of();
        int base = Math.max(0, fallbackAvg - 4);
        return List.of(base, base + 1, base + 2, base + 2, base + 3, base + 4, fallbackAvg);
    }

    // ── 4. Review Queue ─────────────────────────────────────

    @Cacheable(value = CacheNames.DASHBOARD_REVIEW_QUEUE, key = "#callerIdx")
    public List<ReviewQueueItemDto> reviewQueue(Long callerIdx) {
        User caller = findUser(callerIdx);
        Scope scope = resolveScope(caller);
        return filterTasksForScope(scope).stream()
                .filter(t -> t.getStatus() == TaskStatus.REVIEW)
                .map(this::toReviewItem)
                .toList();
    }

    private ReviewQueueItemDto toReviewItem(Task t) {
        Long campaignId = (t.getTaskPart() != null && t.getTaskPart().getCampaign() != null)
                ? t.getTaskPart().getCampaign().getIdx() : null;
        String campaignName = (t.getTaskPart() != null && t.getTaskPart().getCampaign() != null)
                ? t.getTaskPart().getCampaign().getName() : null;
        return new ReviewQueueItemDto(
                t.getIdx(),
                t.getName(),
                campaignId,
                campaignName,
                t.getAssignee() != null ? t.getAssignee().getIdx() : null,
                t.getAssignee() != null ? t.getAssignee().getName() : null,
                t.getDueDate(),
                t.getPriority() != null ? t.getPriority().name() : null
        );
    }

    // ── 5. Blockers ─────────────────────────────────────────

    @Cacheable(value = CacheNames.DASHBOARD_BLOCKERS, key = "#callerIdx")
    public List<BlockerDto> blockers(Long callerIdx) {
        User caller = findUser(callerIdx);
        Scope scope = resolveScope(caller);
        List<Campaign> visibleCampaigns = filterCampaigns(scope);

        List<BlockerDto> result = new java.util.ArrayList<>();

        // BLOCKED Task
        for (Task t : filterTasksForScope(scope)) {
            if (t.getStatus() == TaskStatus.BLOCKED) {
                Long campaignId = (t.getTaskPart() != null && t.getTaskPart().getCampaign() != null)
                        ? t.getTaskPart().getCampaign().getIdx() : null;
                String campaignName = (t.getTaskPart() != null && t.getTaskPart().getCampaign() != null)
                        ? t.getTaskPart().getCampaign().getName() : null;
                result.add(new BlockerDto(
                        "TASK_BLOCKED",
                        t.getIdx(),
                        t.getName(),
                        campaignId,
                        campaignName,
                        "차단된 업무"
                ));
            }
        }

        // GM 미배정 캠페인
        for (Campaign c : visibleCampaigns) {
            boolean hasGm = memberRepository.findAllByCampaignIdx(c.getIdx()).stream()
                    .anyMatch(m -> m.getCampaignRole() == CampaignMemberRole.GENERAL_MANAGER);
            if (!hasGm) {
                result.add(new BlockerDto(
                        "CAMPAIGN_NO_GM",
                        c.getIdx(),
                        c.getName(),
                        c.getIdx(),
                        c.getName(),
                        "GM이 배정되지 않은 캠페인"
                ));
            }
        }

        return result;
    }

    // ── 6. Asset Categories ────────────────────────────────

    @Cacheable(value = CacheNames.DASHBOARD_ASSET_CATEGORIES, key = "#callerIdx")
    public Map<String, Long> assetCategories(Long callerIdx) {
        User caller = findUser(callerIdx);
        Scope scope = resolveScope(caller);
        Set<Long> visibleCampaignIds = filterCampaigns(scope).stream()
                .map(Campaign::getIdx).collect(Collectors.toSet());

        return assetRepository.findAll().stream()
                .filter(a -> {
                    if (scope.allCampaigns) return true;
                    Long camp = a.getCampaign() != null ? a.getCampaign().getIdx() : null;
                    Long org = a.getOrganization() != null ? a.getOrganization().getIdx() : null;
                    return (camp != null && visibleCampaignIds.contains(camp))
                            || (scope.ownerOrgId != null && Objects.equals(scope.ownerOrgId, org));
                })
                .collect(Collectors.groupingBy(
                        a -> a.getCategory() == null ? "UNKNOWN" : a.getCategory(),
                        Collectors.counting()));
    }

    // ── 7. KPI Categories ──────────────────────────────────

    @Cacheable(value = CacheNames.DASHBOARD_KPI_CATEGORIES, key = "#callerIdx")
    public Map<String, Long> kpiCategories(Long callerIdx) {
        User caller = findUser(callerIdx);
        Scope scope = resolveScope(caller);
        Set<Long> visibleCampaignIds = filterCampaigns(scope).stream()
                .map(Campaign::getIdx).collect(Collectors.toSet());

        return campaignKpiRepository.findAll().stream()
                .filter(k -> visibleCampaignIds.contains(k.getCampaign().getIdx()))
                .collect(Collectors.groupingBy(
                        k -> k.getCategory() == null ? "UNKNOWN" : k.getCategory().name(),
                        Collectors.counting()));
    }

    // ── 권한별 Scope 분기 ───────────────────────────────────

    /**
     * Phase D — 권한별 분기.
     * - HQ + ROLE_ADMIN/GENERAL_MANAGER: 전체
     * - AFFILIATE / EXTERNAL_PARTNER: 본인 조직 참여 캠페인만
     * - 그 외 (ROLE_USER 등 STAFF): assignee = 본인인 task / member로 참여한 캠페인만
     */
    private Scope resolveScope(User caller) {
        Organization org = caller.getOrganization();
        OrganizationType orgType = org == null ? null : org.getType();
        String role = caller.getRole();

        boolean isAdminLike = "ROLE_ADMIN".equals(role)
                || "ROLE_GENERAL_MANAGER".equals(role)
                || "ROLE_MANAGER".equals(role);

        if (orgType == OrganizationType.HQ && isAdminLike) {
            return new Scope("HQ", true, null, caller.getIdx(), false);
        }
        if (orgType == OrganizationType.AFFILIATE && isAdminLike) {
            return new Scope("AFFILIATE", false, org.getIdx(), caller.getIdx(), false);
        }
        if (orgType == OrganizationType.EXTERNAL_PARTNER && isAdminLike) {
            return new Scope("EXTERNAL_PARTNER", false, org.getIdx(), caller.getIdx(), false);
        }
        // STAFF (ROLE_USER 또는 매니저급이 아닌 경우): 본인 캠페인만
        return new Scope("STAFF", false, org == null ? null : org.getIdx(), caller.getIdx(), true);
    }

    private Long resolveOwnerOrgIdFilter(Scope scope) {
        if (scope.allCampaigns) return null;          // 전체
        if (scope.staffOnly) return scope.ownerOrgId; // staff는 본인 조직 KPI까지 보이게
        return scope.ownerOrgId;
    }

    private List<Campaign> filterCampaigns(Scope scope) {
        if (scope.allCampaigns) {
            return campaignRepository.findAll();
        }
        if (scope.staffOnly) {
            // staff: member로 참여한 캠페인
            return memberRepository.findAllWithCampaignByUserIdx(scope.callerUserIdx).stream()
                    .map(CampaignMember::getCampaign)
                    .distinct()
                    .toList();
        }
        if (scope.ownerOrgId != null) {
            return participantRepository.findCampaignsByOrganizationIdx(scope.ownerOrgId);
        }
        return List.of();
    }

    private List<Task> filterTasksForScope(Scope scope) {
        if (scope.allCampaigns) {
            return taskRepository.findAllByOrderByIdxDesc();
        }
        if (scope.staffOnly) {
            return taskRepository.findAllByAssignee_IdxOrderByIdxDesc(scope.callerUserIdx);
        }
        if (scope.ownerOrgId != null) {
            List<Long> campaignIds = participantRepository.findCampaignsByOrganizationIdx(scope.ownerOrgId).stream()
                    .map(Campaign::getIdx).toList();
            if (campaignIds.isEmpty()) return List.of();
            return taskRepository.findAllByTaskPart_Campaign_IdxInOrderByIdxDesc(campaignIds);
        }
        return List.of();
    }

    // ── Helper ──────────────────────────────────────────────

    private User findUser(Long userIdx) {
        if (userIdx == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found.");
        }
        return userRepository.findById(userIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }

    private static Integer averageAchievement(List<CampaignKpi> kpis) {
        List<Integer> percents = kpis.stream()
                .map(k -> calcPercent(k.getActualValue(), k.getTargetValue()))
                .filter(Objects::nonNull)
                .toList();
        if (percents.isEmpty()) return null;
        int sum = percents.stream().mapToInt(Integer::intValue).sum();
        return sum / percents.size();
    }

    private static Integer calcPercent(BigDecimal actual, BigDecimal target) {
        if (actual == null) return null;
        if (target == null || target.compareTo(BigDecimal.ZERO) == 0) return 0;
        return actual.multiply(BigDecimal.valueOf(100))
                .divide(target, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private static String nullIfBlank(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 권한별 스코프 정보.
     * @param label              표시용 이름 (HQ / AFFILIATE / EXTERNAL_PARTNER / STAFF)
     * @param allCampaigns       모든 캠페인을 볼 수 있는가
     * @param ownerOrgId         조직 단위 필터를 적용할 OrganizationId (null이면 적용 안 함)
     * @param callerUserIdx      호출 유저 idx
     * @param staffOnly          본인 캠페인만 보이는가
     */
    private record Scope(
            String label,
            boolean allCampaigns,
            Long ownerOrgId,
            Long callerUserIdx,
            boolean staffOnly
    ) {}
}
