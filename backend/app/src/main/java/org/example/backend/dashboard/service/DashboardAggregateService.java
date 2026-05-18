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
    private final org.example.backend.common.cache.UserAuthCache userAuthCache;
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
            key = "#callerIdx + ':' + (#periodCode == null ? '' : #periodCode)")
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

        // AFFILIATE / EXTERNAL_PARTNER 스코프: 전사 OrgKpi 평균 — ⚡ B1: N sum 쿼리 → 1 GROUP BY
        Integer companyAvgPct = null;
        if ("AFFILIATE".equals(scope.label) || "EXTERNAL_PARTNER".equals(scope.label)) {
            List<OrganizationKpi> allActive = orgKpiRepository.findByFilters(null, null, GoalStatus.ACTIVE);
            if (!allActive.isEmpty()) {
                Map<Long, BigDecimal> actualByKpi = loadActualSumMap(
                        allActive.stream().map(OrganizationKpi::getIdx).toList());
                List<Integer> percents = allActive.stream()
                        .map(k -> calcPercent(
                                actualByKpi.getOrDefault(k.getIdx(), BigDecimal.ZERO),
                                k.getTargetValue()))
                        .filter(Objects::nonNull)
                        .toList();
                companyAvgPct = percents.isEmpty() ? null
                        : percents.stream().mapToInt(Integer::intValue).sum() / percents.size();
            }
        }

        // trend (지난주 대비 %p): 실 데이터 (지난주 OrgKpi snapshot) 없으면 null.
        // frontend 는 null 일 때 "지난주" 표시 안 함.
        Integer trend = null;

        return new DashboardSummaryDto(
                active, avg, avg,
                pending, partnerCount, newPartnerCount, rfpCount,
                trend, companyAvgPct, miniStats, scope.label
        );
    }

    /** B1 헬퍼: 여러 OrgKpi 의 actual sum 일괄 조회. */
    private Map<Long, BigDecimal> loadActualSumMap(java.util.Collection<Long> orgKpiIds) {
        if (orgKpiIds == null || orgKpiIds.isEmpty()) return Map.of();
        Map<Long, BigDecimal> out = new HashMap<>();
        for (Object[] row : contributionRepository.sumByOrgKpiIdxIn(orgKpiIds)) {
            if (row == null || row.length < 3 || row[0] == null) continue;
            out.put((Long) row[0], row[2] == null ? BigDecimal.ZERO : (BigDecimal) row[2]);
        }
        return out;
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
            Long kpiId = (Long) row[0];
            committedByKpi.put(kpiId, row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1]);
            actualByKpi.put(kpiId, row[2] == null ? BigDecimal.ZERO : (BigDecimal) row[2]);
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
