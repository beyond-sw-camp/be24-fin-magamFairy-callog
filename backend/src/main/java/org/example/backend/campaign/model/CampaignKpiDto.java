package org.example.backend.campaign.model;

import org.example.backend.user.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

public class CampaignKpiDto {

    public record Res(
            Long idx,
            String name,
            KpiCategory category,
            BigDecimal targetValue,
            BigDecimal actualValue,
            String unit,
            String ownerLabel,
            Long ownerUserIdx,
            String ownerUserName,
            String memo,
            String nextAction,
            LocalDateTime measuredAt,
            Integer achievementPercent,
            KpiStatus status
    ) {
        public static Res from(CampaignKpi k) {
            Integer pct = calcAchievement(k.getActualValue(), k.getTargetValue());
            User owner = k.getOwnerUser();
            return new Res(
                    k.getIdx(), k.getName(), k.getCategory(),
                    k.getTargetValue(), k.getActualValue(), k.getUnit(),
                    k.getOwnerLabel(),
                    owner != null ? owner.getIdx() : null,
                    owner != null ? owner.getName() : null,
                    k.getMemo(), k.getNextAction(), k.getMeasuredAt(),
                    pct, calcStatus(k.getActualValue(), pct)
            );
        }
    }

    public record Summary(
            Integer overallAchievementPercent,
            BigDecimal totalImpression,
            BigDecimal totalClicks,
            Integer totalKpiCount,
            Integer achievedCount,
            Integer pendingCount
    ) {}

    public record ListRes(
            List<Res> items,
            Summary summary,
            String kpiAnalysis,
            boolean editable
    ) {}

    public record CreateReq(
            String name,
            KpiCategory category,
            BigDecimal targetValue,
            String unit,
            String ownerLabel,
            Long ownerUserIdx
    ) {}

    public record UpdateMetaReq(
            String name,
            KpiCategory category,
            BigDecimal targetValue,
            String unit,
            String ownerLabel,
            Long ownerUserIdx
    ) {}

    public record UpdateActualReq(
            BigDecimal actualValue,
            String memo,
            String nextAction
    ) {}

    public record UpdateAnalysisReq(String kpiAnalysis) {}

    public record ImportFrameworkReq(String frameworkKey) {}

    public record FrameworkRes(
            String key,
            String name,
            String description,
            List<FrameworkItem> items
    ) {}

    public record FrameworkItem(
            String name,
            KpiCategory category,
            BigDecimal defaultTarget,
            String unit
    ) {}

    // ── 내부 계산 유틸 ──────────────────────────────────────
    public static Integer calcAchievement(BigDecimal actual, BigDecimal target) {
        if (actual == null) return null;
        if (target == null || target.compareTo(BigDecimal.ZERO) == 0) return 0;
        return actual.multiply(BigDecimal.valueOf(100))
                .divide(target, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    public static KpiStatus calcStatus(BigDecimal actual, Integer pct) {
        if (actual == null) return KpiStatus.PENDING;
        if (pct != null && pct >= 120) return KpiStatus.OVER;
        if (pct != null && pct >= 100) return KpiStatus.ACHIEVED;
        return KpiStatus.BEHIND;
    }
}
