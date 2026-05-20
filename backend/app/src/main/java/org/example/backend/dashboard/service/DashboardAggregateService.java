package org.example.backend.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.activity.model.CampaignActivity;
import org.example.backend.activity.repository.CampaignActivityRepository;
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
import org.example.backend.dashboard.dto.CampaignProgressDto;
import org.example.backend.dashboard.dto.DashboardSummaryDto;
import org.example.backend.dashboard.dto.LabelValueDto;
import org.example.backend.dashboard.dto.PartnerProgressDto;
import org.example.backend.dashboard.dto.PipelineStageDto;
import org.example.backend.dashboard.dto.QuarterGoalProgressDto;
import org.example.backend.dashboard.dto.RecentActivityDto;
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
    private final org.example.backend.common.redis.UserAuthCache userAuthCache;
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
    private final CampaignActivityRepository activityRepository;

    // ── 0. Dashboard 페이지 통합 응답 (B4 + Fix A) ───────────────────
    /**
     * Dashboard 페이지 진입 시 5종 데이터를 한 번에 산출 + 캐시.
     *
     * ⚡ Fix A: 메소드 자체에 @Cacheable.
     *   - 이전: loadAll() 안에서 summary() / quarterGoals() 등 같은 클래스 호출 →
     *     Spring AOP proxy 우회 → sub-method 의 @Cacheable 무시 → 캐시 효과 거의 없었음.
     *   - 현재: 통합 응답 (DashboardPageDto) 자체를 단일 키로 Redis 에 적재.
     *     cache hit 시 sub-method 진입조차 안 함 (5종 DB 쿼리 전부 skip).
     *
     * 캐시 키: "{callerIdx}:{periodCode}" — 사용자별 + 분기별 분리.
     * Invalidation: 캠페인/KPI/Task 변경 시 @CacheEvict(DASHBOARD_PAGE, allEntries=true) 필요.
     */
    @Cacheable(value = CacheNames.DASHBOARD_PAGE,
            key = "#callerIdx + ':' + (#periodCode == null ? '' : #periodCode)",
            sync = true)   // ⚡ Cache Stampede 방지: 동일 키 DB 로딩을 한 스레드만 수행 (cold-start DB 폭주 차단)
    public org.example.backend.dashboard.dto.DashboardPageDto loadAll(Long callerIdx, String periodCode) {
        return new org.example.backend.dashboard.dto.DashboardPageDto(
                summary(callerIdx),
                quarterGoals(callerIdx, periodCode),
                partnerProgress(callerIdx),
                assetCategories(callerIdx),
                kpiCategories(callerIdx)
        );
    }

    // ── 1. Summary ──────────────────────────────────────────

    @Cacheable(value = CacheNames.DASHBOARD_SUMMARY, key = "#callerIdx")
    public DashboardSummaryDto summary(Long callerIdx) {
        User caller = findUser(callerIdx);
        Scope scope = resolveScope(caller);
        List<Campaign> visibleCampaigns = filterCampaigns(scope);
        // F1: 본인 조직 idx — 협력사 카운트에서 제외
        Long myOrgIdx = caller.getOrganization() == null ? null : caller.getOrganization().getIdx();

        long active = visibleCampaigns.stream()
                .filter(c -> ACTIVE_CAMPAIGN_STATUSES.contains(c.getStatus()))
                .count();

        Set<Long> visibleCampaignIds = visibleCampaigns.stream()
                .map(Campaign::getIdx).collect(Collectors.toSet());
        // ⚡ B2: findAll() 풀스캔 → IN 쿼리
        List<CampaignKpi> kpis = visibleCampaignIds.isEmpty()
                ? List.of()
                : campaignKpiRepository.findAllByCampaign_IdxInOrderByIdxAsc(visibleCampaignIds);
        Integer avg = averageAchievement(kpis);

        // 검수 대기 / 패스율
        List<Task> scopedTasks = filterTasksForScope(scope);
        long pending = scopedTasks.stream().filter(t -> t.getStatus() == TaskStatus.REVIEW).count();
        long doneCnt = scopedTasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        long reviewedTotal = pending + doneCnt;
        Integer passPct = reviewedTotal == 0 ? null
                : (int) Math.round(doneCnt * 100.0 / reviewedTotal);

        // ⚡ B3 + F1: 캠페인 N개 × 1쿼리(루프) → 1 IN 쿼리. + 본인 조직 제외.
        Set<Long> partnerOrgIds = new HashSet<>();
        if (!visibleCampaignIds.isEmpty()) {
            for (CampaignParticipant cp : participantRepository.findAllByCampaignIdxInWithOrg(visibleCampaignIds)) {
                if (cp.getOrganization() == null) continue;
                Long orgIdx = cp.getOrganization().getIdx();
                if (myOrgIdx != null && Objects.equals(myOrgIdx, orgIdx)) continue;
                partnerOrgIds.add(orgIdx);
            }
        }
        long partnerCount = partnerOrgIds.size();

        // ⚡ B2: findAll().stream().filter().count() → COUNT 쿼리 한 방
        long liveAssetCount = scope.allCampaigns
                ? assetRepository.countAllAssets()
                : (visibleCampaignIds.isEmpty() && scope.ownerOrgId == null
                        ? 0L
                        : assetRepository.countVisibleAssets(
                                visibleCampaignIds.isEmpty() ? Set.of(-1L) : visibleCampaignIds,
                                scope.ownerOrgId));

        Integer matchAvg = avg;
        long rfpCount = benefitRepository.count();

        // 신규 협력사 — 본인 조직 제외한 partnerOrgIds 중 createdAt가 30일 내 (F1)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long newPartnerCount = partnerOrgIds.isEmpty() ? 0L
                : organizationRepository.findAllById(partnerOrgIds).stream()
                        .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(thirtyDaysAgo))
                        .count();

        List<DashboardSummaryDto.MiniStat> miniStats = List.of(
                new DashboardSummaryDto.MiniStat("검수 패스율", passPct == null ? "-" : passPct + "%"),
                new DashboardSummaryDto.MiniStat("매칭 평균", matchAvg == null ? "-" : matchAvg + "점"),
                new DashboardSummaryDto.MiniStat("자산 LIVE", String.valueOf(liveAssetCount))
        );

        // trend (지난주 대비 %p): 실 데이터 (지난주 OrgKpi snapshot) 없으면 null.
        // frontend 는 null 일 때 "지난주" 표시 안 함.
        Integer trend = null;

        return new DashboardSummaryDto(
                active, avg, avg,
                pending, partnerCount, newPartnerCount, rfpCount,
                trend, miniStats, scope.label
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

        if (kpis.isEmpty()) return List.of();

        // ⚡ B1: OrgKpi N개 × 2(sumCommitted+sumActual) → 1 GROUP BY
        List<Long> kpiIds = kpis.stream().map(OrganizationKpi::getIdx).toList();
        Map<Long, BigDecimal> committedByKpi = new HashMap<>();
        Map<Long, BigDecimal> actualByKpi = new HashMap<>();
        for (Object[] row : contributionRepository.sumByOrgKpiIdxIn(kpiIds)) {
            if (row == null || row.length < 3 || row[0] == null) continue;
            // COALESCE(SUM(...),0) 의 반환 타입이 DB/Hibernate 버전에 따라 BigDecimal 이 아닐 수 있어
            // 강제 캐스팅 대신 Number → BigDecimal 변환 (ClassCastException 방지).
            Long kpiId = ((Number) row[0]).longValue();
            committedByKpi.put(kpiId, toBigDecimal(row[1]));
            actualByKpi.put(kpiId, toBigDecimal(row[2]));
        }

        return kpis.stream()
                .map(k -> toQuarterGoalDto(k,
                        committedByKpi.getOrDefault(k.getIdx(), BigDecimal.ZERO),
                        actualByKpi.getOrDefault(k.getIdx(), BigDecimal.ZERO)))
                .toList();
    }

    private QuarterGoalProgressDto toQuarterGoalDto(OrganizationKpi k, BigDecimal committed, BigDecimal actual) {
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
                actual,                     // ← record 필드명이 actualValue 로 변경됨 (frontend Z1/P2 와 일치)
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
        // F1: 본인 조직 idx — Top5 협력사에서 자기 자신은 제외
        Long myOrgIdx = caller.getOrganization() == null ? null : caller.getOrganization().getIdx();

        if (visibleCampaigns.isEmpty()) return List.of();
        Set<Long> campaignIds = visibleCampaigns.stream()
                .map(Campaign::getIdx).collect(Collectors.toSet());

        // ⚡ B3: 캠페인 N개 × 2쿼리 → 2개 IN 쿼리
        List<CampaignParticipant> allParticipants =
                participantRepository.findAllByCampaignIdxInWithOrg(campaignIds);
        List<CampaignKpi> allCampaignKpis =
                campaignKpiRepository.findAllByCampaign_IdxInOrderByIdxAsc(campaignIds);

        Map<Long, List<CampaignKpi>> kpisByCampaign = allCampaignKpis.stream()
                .collect(Collectors.groupingBy(k -> k.getCampaign().getIdx()));
        Map<Long, Campaign> campaignById = visibleCampaigns.stream()
                .collect(Collectors.toMap(Campaign::getIdx, c -> c, (a, b) -> a));

        Map<Long, Organization> partners = new HashMap<>();
        Map<Long, Long> totalByOrg = new HashMap<>();
        Map<Long, Long> activeByOrg = new HashMap<>();
        Map<Long, List<CampaignKpi>> kpisByOrg = new HashMap<>();

        for (CampaignParticipant cp : allParticipants) {
            Organization org = cp.getOrganization();
            if (org == null) continue;
            // F1: 본인 조직 제외
            if (myOrgIdx != null && Objects.equals(myOrgIdx, org.getIdx())) continue;

            Long orgIdx = org.getIdx();
            partners.putIfAbsent(orgIdx, org);
            totalByOrg.merge(orgIdx, 1L, Long::sum);
            Campaign c = campaignById.get(cp.getCampaign().getIdx());
            if (c != null && ACTIVE_CAMPAIGN_STATUSES.contains(c.getStatus())) {
                activeByOrg.merge(orgIdx, 1L, Long::sum);
            }
            kpisByOrg.computeIfAbsent(orgIdx, k -> new java.util.ArrayList<>())
                    .addAll(kpisByCampaign.getOrDefault(cp.getCampaign().getIdx(), List.of()));
        }

        LocalDate sevenDaysAgo = LocalDate.now().minusDays(6);
        return partners.values().stream()
                .map(org -> {
                    Integer avgPct = averageAchievement(kpisByOrg.getOrDefault(org.getIdx(), List.of()));
                    List<Integer> recent7d = recent7dFromSnapshots(org.getIdx(), sevenDaysAgo);
                    Integer delta = computeDeltaFromRecent(recent7d);
                    return new PartnerProgressDto(
                            org.getIdx(),
                            org.getName(),
                            totalByOrg.getOrDefault(org.getIdx(), 0L),
                            activeByOrg.getOrDefault(org.getIdx(), 0L),
                            avgPct,
                            delta,
                            recent7d
                    );
                })
                .toList();
    }

    /**
     * 최근 7일 daily snapshot 조회. 7개 미만이면 빈 배열 반환 (가짜 stub 생성 금지).
     * Frontend Z2 sparkline 은 빈 배열 시 표시 생략.
     */
    private List<Integer> recent7dFromSnapshots(Long orgId, LocalDate from) {
        List<KpiDailySnapshot> snaps = dailySnapshotRepository
                .findAllByOrganization_IdxAndDateGreaterThanEqualOrderByDateAsc(orgId, from);
        if (snaps.size() < 7) return List.of();
        return snaps.stream()
                .skip(Math.max(0, snaps.size() - 7))
                .map(s -> s.getAvgKpiPercent() == null ? 0 : s.getAvgKpiPercent())
                .toList();
    }

    /**
     * recent7d 의 첫 값 → 마지막 값 차이 (%p). 데이터 부족 시 null.
     * Frontend Z2 "Δ +N" 배지 표시용. null 이면 배지 안 그림 ("안정" 으로 폴백되지 않게 됨).
     */
    private static Integer computeDeltaFromRecent(List<Integer> recent7d) {
        if (recent7d == null || recent7d.size() < 2) return null;
        return recent7d.get(recent7d.size() - 1) - recent7d.get(0);
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

        // ⚡ B2: findAll().stream().filter().groupingBy (풀스캔) → DB-level GROUP BY.
        // category 도 LOWER() 로 정규화하여 frontend lowercase 키와 자동 매칭.
        List<Object[]> rows;
        if (scope.allCampaigns) {
            rows = assetRepository.countCategoriesAll();
        } else {
            Set<Long> visibleCampaignIds = filterCampaigns(scope).stream()
                    .map(Campaign::getIdx).collect(Collectors.toSet());
            if (visibleCampaignIds.isEmpty() && scope.ownerOrgId == null) return Map.of();
            rows = assetRepository.countCategoriesVisible(
                    visibleCampaignIds.isEmpty() ? Set.of(-1L) : visibleCampaignIds,
                    scope.ownerOrgId);
        }
        Map<String, Long> out = new HashMap<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) continue;
            out.put((String) row[0], ((Number) row[1]).longValue());
        }
        return out;
    }

    // ── 7. KPI Categories ──────────────────────────────────

    @Cacheable(value = CacheNames.DASHBOARD_KPI_CATEGORIES, key = "#callerIdx")
    public Map<String, Long> kpiCategories(Long callerIdx) {
        User caller = findUser(callerIdx);
        Scope scope = resolveScope(caller);
        Set<Long> visibleCampaignIds = filterCampaigns(scope).stream()
                .map(Campaign::getIdx).collect(Collectors.toSet());

        if (visibleCampaignIds.isEmpty()) return Map.of();

        // ⚡ B2: findAll() 풀스캔 → IN 쿼리. 메모리에서 카테고리 GROUP BY.
        List<CampaignKpi> kpis = campaignKpiRepository.findAllByCampaign_IdxInOrderByIdxAsc(visibleCampaignIds);
        return kpis.stream()
                .collect(Collectors.groupingBy(
                        k -> k.getCategory() == null ? "UNKNOWN" : k.getCategory().name(),
                        Collectors.counting()));
    }

    // ── 8. Recent Activity (Zone1 P1 우) ───────────────────

    /**
     * 호출자가 참여/담당한 캠페인들의 최근 활동 피드 (최신순, 최대 20건).
     * 회사 격리: filterCampaigns(scope) 가 반환한 캠페인만 대상.
     */
    public List<RecentActivityDto> recentActivity(Long callerIdx) {
        User caller = findUser(callerIdx);
        Scope scope = resolveScope(caller);
        Set<Long> campaignIds = filterCampaigns(scope).stream()
                .map(Campaign::getIdx).collect(Collectors.toSet());
        if (campaignIds.isEmpty()) return List.of();

        return activityRepository.findRecentByCampaignIdxIn(
                        campaignIds, org.springframework.data.domain.PageRequest.of(0, 20))
                .stream()
                .map(this::toRecentActivityDto)
                .toList();
    }

    private RecentActivityDto toRecentActivityDto(CampaignActivity a) {
        return new RecentActivityDto(
                a.getIdx(),
                a.getCampaign() != null ? a.getCampaign().getIdx() : null,
                a.getCampaign() != null ? a.getCampaign().getName() : null,
                a.getType(),
                a.getDescription(),
                a.getActor() != null ? a.getActor().getName() : null,
                a.getCreatedAt()
        );
    }

    // ── 9. Campaign Pipeline 퍼널 (Zone4 P1) ────────────────

    private static final List<String> PIPELINE_ORDER = List.of("draft", "live", "review", "paused", "completed");
    private static final Map<String, String> PIPELINE_LABELS = Map.of(
            "draft", "기획",
            "live", "실행",
            "review", "검수",
            "paused", "보류",
            "completed", "완료");

    /**
     * 내 캠페인들을 status 별로 count 하여 퍼널 단계 순서로 반환.
     * 알 수 없는 status 는 원본 문자열을 stage 라벨로 사용 (맨 뒤).
     */
    public List<PipelineStageDto> campaignPipeline(Long callerIdx) {
        User caller = findUser(callerIdx);
        Scope scope = resolveScope(caller);
        Map<String, Long> countByStatus = filterCampaigns(scope).stream()
                .collect(Collectors.groupingBy(
                        c -> c.getStatus() == null ? "draft" : c.getStatus(),
                        Collectors.counting()));
        if (countByStatus.isEmpty()) return List.of();

        List<PipelineStageDto> result = new ArrayList<>();
        for (String status : PIPELINE_ORDER) {
            long cnt = countByStatus.getOrDefault(status, 0L);
            if (cnt > 0) {
                result.add(new PipelineStageDto(PIPELINE_LABELS.getOrDefault(status, status), cnt));
            }
        }
        // 정의되지 않은 status (예: closed 등) 는 뒤에 원본 라벨로 추가
        countByStatus.entrySet().stream()
                .filter(e -> !PIPELINE_ORDER.contains(e.getKey()))
                .forEach(e -> result.add(new PipelineStageDto(e.getKey(), e.getValue())));
        return result;
    }

    // ── 10. Campaign Progress 랭킹 (Zone2) ──────────────────

    /**
     * 내 캠페인별 진척률(DONE task / 전체 task)을 내림차순 정렬해 반환.
     * isMine = 호출자가 해당 캠페인의 PM(MANAGER) 또는 GM 인 경우 true.
     */
    public List<CampaignProgressDto> campaignProgress(Long callerIdx) {
        User caller = findUser(callerIdx);
        Scope scope = resolveScope(caller);
        List<Campaign> visibleCampaigns = filterCampaigns(scope);
        if (visibleCampaigns.isEmpty()) return List.of();

        Set<Long> campaignIds = visibleCampaigns.stream()
                .map(Campaign::getIdx).collect(Collectors.toSet());

        // 캠페인별 [total, done] 1 GROUP BY
        Map<Long, long[]> stats = new HashMap<>();
        for (Object[] row : taskRepository.countTotalAndDoneByCampaignIdxIn(campaignIds)) {
            if (row == null || row.length < 3 || row[0] == null) continue;
            long cid = ((Number) row[0]).longValue();
            long total = row[1] == null ? 0L : ((Number) row[1]).longValue();
            long done = row[2] == null ? 0L : ((Number) row[2]).longValue();
            stats.put(cid, new long[]{total, done});
        }

        // 호출자가 PM/GM 인 캠페인 집합
        Set<Long> myManagedCampaignIds = memberRepository.findAllWithCampaignByUserIdx(callerIdx).stream()
                .filter(m -> m.getCampaignRole() == CampaignMemberRole.MANAGER
                        || m.getCampaignRole() == CampaignMemberRole.GENERAL_MANAGER)
                .map(m -> m.getCampaign().getIdx())
                .collect(Collectors.toSet());

        return visibleCampaigns.stream()
                .map(c -> {
                    long[] s = stats.getOrDefault(c.getIdx(), new long[]{0L, 0L});
                    int pct = s[0] == 0 ? 0 : (int) Math.round(s[1] * 100.0 / s[0]);
                    return new CampaignProgressDto(
                            c.getIdx(),
                            c.getName(),
                            c.getColor(),
                            myManagedCampaignIds.contains(c.getIdx()),
                            pct);
                })
                .sorted((a, b) -> Integer.compare(b.completionPct(), a.completionPct()))
                .toList();
    }

    // ── 11. Revenue Trend (Zone4 P2) ────────────────────────

    /**
     * REVENUE 카테고리 KPI 의 월별 실적 추이 (최근 6개월).
     * 내 조직 소유 REVENUE OrganizationKpi 의 KpiMonthlySnapshot.actual 을 월별 합산.
     * snapshot 데이터가 없으면 빈 리스트 (가짜 값 생성 금지).
     */
    public List<LabelValueDto> revenueTrend(Long callerIdx) {
        User caller = findUser(callerIdx);
        Scope scope = resolveScope(caller);

        List<OrganizationKpi> revenueKpis = orgKpiRepository.findRevenueKpis(
                scope.allCampaigns ? null : scope.ownerOrgId);
        if (revenueKpis.isEmpty()) return List.of();

        // 최근 6개월 (year-month) 키 순서 준비
        LocalDate now = LocalDate.now();
        List<int[]> months = new ArrayList<>(6); // [year, month]
        for (int i = 5; i >= 0; i--) {
            LocalDate ym = now.minusMonths(i);
            months.add(new int[]{ym.getYear(), ym.getMonthValue()});
        }

        // KPI별 (year,month)->actual 합산
        Map<String, BigDecimal> actualByYm = new HashMap<>();
        for (OrganizationKpi kpi : revenueKpis) {
            // 두 연도가 걸칠 수 있어 등장하는 연도 모두 조회
            Set<Integer> years = months.stream().map(m -> m[0]).collect(Collectors.toSet());
            for (Integer year : years) {
                for (KpiMonthlySnapshot s : monthlySnapshotRepository
                        .findAllByOrgKpi_IdxAndYearOrderByMonthAsc(kpi.getIdx(), year)) {
                    if (s.getActualValue() == null) continue;
                    String key = s.getYear() + "-" + s.getMonth();
                    actualByYm.merge(key, s.getActualValue(), BigDecimal::add);
                }
            }
        }
        if (actualByYm.isEmpty()) return List.of();

        List<LabelValueDto> result = new ArrayList<>(6);
        for (int[] ym : months) {
            String key = ym[0] + "-" + ym[1];
            BigDecimal val = actualByYm.getOrDefault(key, BigDecimal.ZERO);
            result.add(new LabelValueDto(String.format("%d-%02d", ym[0], ym[1]), val));
        }
        return result;
    }

    // ── 권한별 Scope 분기 ───────────────────────────────────

    /**
     * 권한별 분기 (회사 격리 — 모든 조직이 자기 조직 참여 캠페인만).
     * - HQ + ADMIN/GM/MANAGER: 자기(HQ) 조직이 participant인 캠페인만 (= 만든 캠페인 PM + 참여 캠페인)
     * - AFFILIATE / EXTERNAL_PARTNER + 관리자: 본인 조직 참여 캠페인만
     * - 그 외 (ROLE_USER 등 STAFF): assignee = 본인인 task / member로 참여한 캠페인만
     *
     * ※ 이전엔 HQ가 allCampaigns=true(전사)였으나, "자기 회사만" 정책에 따라 제거.
     *   HQ도 다른 조직과 동일하게 ownerOrgId(participant) 기반.
     */
    private Scope resolveScope(User caller) {
        Organization org = caller.getOrganization();
        OrganizationType orgType = org == null ? null : org.getType();
        String role = caller.getRole();

        boolean isAdminLike = "ROLE_ADMIN".equals(role)
                || "ROLE_GENERAL_MANAGER".equals(role)
                || "ROLE_MANAGER".equals(role);

        if (orgType == OrganizationType.HQ && isAdminLike) {
            // 변경: 전사(allCampaigns=true) 제거 → 자기 조직 participant 캠페인만
            return new Scope("HQ", false, org.getIdx(), caller.getIdx(), false);
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
            // 1급화: 직접 campaign_id 또는 업무파트 경유 둘 다 포함
            return taskRepository.findAllByCampaignIdsDirectOrViaTaskPart(campaignIds);
        }
        return List.of();
    }

    // ── Helper ──────────────────────────────────────────────

    /**
     * ⚡ B5: in-memory cache (60s TTL) 를 통한 user 조회.
     * Dashboard 의 5개 endpoint 가 병렬 호출돼도 첫 호출만 DB, 나머지는 cache hit.
     */
    private User findUser(Long userIdx) {
        return userAuthCache.loadUser(userIdx);
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

    /** COALESCE/SUM 결과(BigDecimal·Long·Double 등 무엇이든)를 안전하게 BigDecimal 로 변환. */
    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return new BigDecimal(n.toString());
        return new BigDecimal(v.toString());
    }

    private static Integer calcPercent(BigDecimal actual, BigDecimal target) {
        if (actual == null) return null;
        if (target == null || target.compareTo(BigDecimal.ZERO) == 0) return 0;
        int pct = actual.multiply(BigDecimal.valueOf(100))
                .divide(target, 0, RoundingMode.HALF_UP)
                .intValue();
        // 🛡 단위 불일치로 비정상 비율 (예: 10000000%) 들어오면 차트가 망가짐 → 9999% 로 clamp.
        if (pct < 0) return 0;
        if (pct > 9999) return 9999;
        return pct;
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
