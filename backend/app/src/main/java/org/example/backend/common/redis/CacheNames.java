package org.example.backend.common.redis;

/**
 * @Cacheable / @CacheEvict 의 value 에 쓰일 캐시 이름 상수.
 * 문자열 오타 방지 — 캐시명이 틀리면 evict 가 안 되는데, 컴파일 단계에서 못 잡으면 보안 사고로 이어질 수 있음.
 *
 * 키 네이밍 컨벤션: "도메인:기능" (콜론 구분)
 *   예) dashboard:summary, campaign:member:role
 */
public final class CacheNames {
    private CacheNames() {}

    // ── 대시보드 (사용자별 키) ──
    public static final String DASHBOARD_SUMMARY          = "dashboard:summary";
    public static final String DASHBOARD_QUARTER_GOALS    = "dashboard:quarter-goals";
    public static final String DASHBOARD_PARTNER_PROGRESS = "dashboard:partner-progress";
    public static final String DASHBOARD_REVIEW_QUEUE     = "dashboard:review-queue";
    public static final String DASHBOARD_BLOCKERS         = "dashboard:blockers";
    public static final String DASHBOARD_ASSET_CATEGORIES = "dashboard:asset-categories";
    public static final String DASHBOARD_KPI_CATEGORIES   = "dashboard:kpi-categories";

    /**
     * ⚡ Fix A: 통합 endpoint /dashboard 의 응답 (DashboardPageDto) 통째 캐시.
     * key = "{callerIdx}:{periodCode}"
     * cache hit 시 sub-method 들의 @Cacheable 거치지 않고 즉시 응답 (self-invocation 우회).
     * 5종 캐시 데이터의 직렬화/역직렬화도 1회로 감소.
     */
    public static final String DASHBOARD_PAGE             = "dashboard:page";

    // ── 대시보드 Zone 신규 (프론트 loadZoneExtras 가 진입 시 호출) ──
    public static final String DASHBOARD_RECENT_ACTIVITY  = "dashboard:recent-activity";  // 실시간성 ↑ → 짧은 TTL
    public static final String DASHBOARD_AD_REVIEW_QUEUE  = "dashboard:ad-review-queue";  // 변경 잦음 → 짧은 TTL
    public static final String DASHBOARD_CAMPAIGN_PIPELINE = "dashboard:campaign-pipeline";
    public static final String DASHBOARD_CAMPAIGN_PROGRESS = "dashboard:campaign-progress";
    public static final String DASHBOARD_REVENUE_YOY      = "dashboard:revenue-yoy";      // 거의 안 변함 → 긴 TTL

    // ── 캠페인 목록 (대시보드 진입 시 ListCampaign — 멤버/참여 변경 시 evict) ──
    public static final String CAMPAIGN_LIST              = "campaign:list";  // key = "{userIdx}:{scope}"

    // ── 캠페인 멤버 권한 (★ 변경 즉시 evict 필수) ──
    public static final String CAMPAIGN_MEMBER_ROLE       = "campaign:member:role";

    // ── 메타데이터 (거의 안 바뀜) ──
    public static final String KPI_TEMPLATES              = "kpi:templates";

    // ── 알림 ──
    public static final String NOTIFICATION_SETTING       = "notification:setting";

    // ── 인증 user 캐시 (Pod 간 공유) ──
    public static final String USER_AUTH            = "user:auth";
}
