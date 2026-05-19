package org.example.backend.dashboard.dto;

import java.util.List;
import java.util.Map;

/**
 * Dashboard 페이지 진입 시 호출되는 5가지 데이터를 한 번에 묶어 응답.
 *
 * 이전: GET /dashboard/summary, /quarter-goals, /partner-progress, /asset-categories, /kpi-categories
 *       = 5개 endpoint 동시 호출 → 매 endpoint 마다 user/scope/visibleCampaigns 5번 재계산
 *
 * 현재: GET /dashboard  ← 단일 호출 → user/scope/visibleCampaigns 1번만 계산해서 5종 데이터 한꺼번에 산출
 *
 * 성능 비교 (HQ ROLE_GENERAL_MANAGER 기준):
 *   - JWT 인증 user fetch: 5회 → 1회
 *   - resolveScope() + filterCampaigns(): 5회 → 1회
 *   - 네트워크 왕복: 5회 → 1회
 *   - 추가로 B1/B2/B3 의 N+1 제거 효과까지 합치면 redis 없이도 충분히 빠름
 */
public record DashboardPageDto(
        DashboardSummaryDto summary,
        List<QuarterGoalProgressDto> quarterGoals,
        List<PartnerProgressDto> partnerProgress,
        Map<String, Long> assetCategories,
        Map<String, Long> kpiCategories
) {}
