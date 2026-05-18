package org.example.backend.common;

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

    // ── 캠페인 멤버 권한 (★ 변경 즉시 evict 필수) ──
    public static final String CAMPAIGN_MEMBER_ROLE       = "campaign:member:role";

    // ── 메타데이터 (거의 안 바뀜) ──
    public static final String KPI_TEMPLATES              = "kpi:templates";

    // ── 알림 ──
    public static final String NOTIFICATION_SETTING       = "notification:setting";
}
