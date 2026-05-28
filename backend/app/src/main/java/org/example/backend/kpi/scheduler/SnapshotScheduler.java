package org.example.backend.kpi.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignKpi;
import org.example.backend.campaign.model.CampaignParticipant;
import org.example.backend.campaign.repository.CampaignKpiRepository;
import org.example.backend.campaign.repository.CampaignParticipantRepository;
import org.example.backend.kpi.model.GoalStatus;
import org.example.backend.kpi.model.KpiDailySnapshot;
import org.example.backend.kpi.model.KpiMonthlySnapshot;
import org.example.backend.kpi.model.OrganizationKpi;
import org.example.backend.kpi.repository.KpiDailySnapshotRepository;
import org.example.backend.kpi.repository.KpiMonthlySnapshotRepository;
import org.example.backend.common.redis.RedisLock;
import org.example.backend.kpi.repository.OrganizationKpiRepository;
import org.example.backend.organization.model.Organization;
import org.example.backend.organization.repository.OrganizationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Phase 2 데이터 모델 보강 — 월별·일별 KPI 스냅샷 배치.
 * - 월별: 매월 1일 02:00, 직전 월의 OrganizationKpi actual/target 저장.
 * - 일별: 매일 02:30, organization별 평균 KPI 달성률 저장 (sparkline 원천).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotScheduler {

    private final OrganizationKpiRepository orgKpiRepository;
    private final KpiMonthlySnapshotRepository monthlyRepo;
    private final KpiDailySnapshotRepository dailyRepo;
    private final OrganizationRepository organizationRepository;
    private final CampaignParticipantRepository participantRepository;
    private final CampaignKpiRepository campaignKpiRepository;
    private final RedisLock redisLock;

    /** 매월 1일 02:00 — 직전 월의 OrgKpi actual/target snapshot. */
    @Scheduled(cron = "0 0 2 1 * *")
    @Transactional
    public void rollupMonthly() {
        String token = redisLock.tryLock("lock:snapshot:monthly", Duration.ofMinutes(10));
        if (token == null) return;   // 다른 Pod이 이미 실행 중 → skip
        try {
            LocalDate target = LocalDate.now().minusMonths(1);
            int year = target.getYear();
            int month = target.getMonthValue();

            int saved = 0;
            for (OrganizationKpi kpi : orgKpiRepository.findAll()) {
                if (kpi.getStatus() != GoalStatus.ACTIVE) continue;
                if (monthlyRepo.findByOrgKpi_IdxAndYearAndMonth(kpi.getIdx(), year, month).isPresent()) continue;
                monthlyRepo.save(KpiMonthlySnapshot.builder()
                        .orgKpi(kpi)
                        .year(year)
                        .month(month)
                        .actualValue(kpi.getActualValue())
                        .targetValue(kpi.getTargetValue())
                        .build());
                saved++;
            }
            log.info("[SnapshotScheduler] monthly {}-{} saved={} rows", year, month, saved);
        } finally {
            redisLock.unLock("lock:snapshot:monthly", token);
        }
    }

    /** 매일 02:30 — organization별 평균 KPI 달성률 snapshot. */
    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void rollupDaily() {
        String token = redisLock.tryLock("lock:snapshot:daily", Duration.ofMinutes(10));
        if (token == null) return;   // 다른 Pod이 이미 실행 중 → skip
        try {
            LocalDate today = LocalDate.now();

            Map<Long, List<CampaignKpi>> kpisByOrg = collectKpisByOrganization();
            int saved = 0;
            for (Organization org : organizationRepository.findAll()) {
                if (dailyRepo.findByOrganization_IdxAndDate(org.getIdx(), today).isPresent()) continue;
                Integer avg = averageAchievement(kpisByOrg.getOrDefault(org.getIdx(), List.of()));
                dailyRepo.save(KpiDailySnapshot.builder()
                        .organization(org)
                        .date(today)
                        .avgKpiPercent(avg)
                        .build());
                saved++;
            }
            log.info("[SnapshotScheduler] daily {} saved={} rows", today, saved);
        } finally {
            redisLock.unLock("lock:snapshot:daily", token);
        }
    }

    /** organization 별로 참여 캠페인의 CampaignKpi 모음. */
    private Map<Long, List<CampaignKpi>> collectKpisByOrganization() {
        Map<Long, List<CampaignKpi>> kpisByOrg = new HashMap<>();
        List<CampaignParticipant> participants = participantRepository.findAll();
        Set<Long> seenCampaigns = participants.stream()
                .map(CampaignParticipant::getCampaign)
                .filter(Objects::nonNull)
                .map(Campaign::getIdx)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

        Map<Long, List<CampaignKpi>> kpisByCampaign = seenCampaigns.isEmpty()
                ? Map.of()
                : campaignKpiRepository.findAllByCampaign_IdxInOrderByIdxAsc(seenCampaigns).stream()
                    .filter(kpi -> kpi.getCampaign() != null && kpi.getCampaign().getIdx() != null)
                    .collect(Collectors.groupingBy(kpi -> kpi.getCampaign().getIdx()));

        for (CampaignParticipant cp : participants) {
            Organization org = cp.getOrganization();
            Campaign campaign = cp.getCampaign();
            if (org == null || org.getIdx() == null || campaign == null || campaign.getIdx() == null) continue;
            List<CampaignKpi> ckpis = kpisByCampaign.getOrDefault(campaign.getIdx(), List.of());
            kpisByOrg.computeIfAbsent(org.getIdx(), k -> new java.util.ArrayList<>()).addAll(ckpis);
        }
        return kpisByOrg;
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
}
