package org.example.backend.dashboard.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.dashboard.service.DashboardAggregateService;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardAggregateService aggregateService;

    /**
     * ⚡ B4: Dashboard 페이지 통합 endpoint.
     * 이전 5개 endpoint (summary, quarter-goals, partner-progress, asset-categories, kpi-categories)
     * 를 한 번의 호출로 묶음. 응답 = DashboardPageDto.
     *
     * Frontend dashboardStore.loadAll() 이 이 endpoint 하나만 호출하도록 변경됨.
     * 기존 개별 endpoint 들은 하위 호환 + nGrinder 부하 테스트용으로 유지.
     */
    @GetMapping
    public ResponseEntity<BaseResponse> dashboardPage(
            @RequestParam(required = false) String period,
            @AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(aggregateService.loadAll(user.getIdx(), period)));
    }

    @GetMapping("/summary")
    public ResponseEntity<BaseResponse> summary(@AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(aggregateService.summary(user.getIdx())));
    }

    @GetMapping("/quarter-goals")
    public ResponseEntity<BaseResponse> quarterGoals(
            @RequestParam(required = false) String period,
            @AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(aggregateService.quarterGoals(user.getIdx(), period)));
    }

    @GetMapping("/partner-progress")
    public ResponseEntity<BaseResponse> partnerProgress(@AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(aggregateService.partnerProgress(user.getIdx())));
    }

    @GetMapping("/review-queue")
    public ResponseEntity<BaseResponse> reviewQueue(@AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(aggregateService.reviewQueue(user.getIdx())));
    }

    @GetMapping("/blockers")
    public ResponseEntity<BaseResponse> blockers(@AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(aggregateService.blockers(user.getIdx())));
    }

    @GetMapping("/asset-categories")
    public ResponseEntity<BaseResponse> assetCategories(@AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(aggregateService.assetCategories(user.getIdx())));
    }

    @GetMapping("/kpi-categories")
    public ResponseEntity<BaseResponse> kpiCategories(@AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(aggregateService.kpiCategories(user.getIdx())));
    }

    /** ⭐NEW: Zone1 P1 우 — 내 참여 캠페인의 최근 활동 피드 (최신순 ~20건). */
    @GetMapping("/recent-activity")
    public ResponseEntity<BaseResponse> recentActivity(@AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(aggregateService.recentActivity(user.getIdx())));
    }

    /** ⭐NEW: Zone4 P1 — 내 캠페인 status별 count 퍼널. */
    @GetMapping("/campaign-pipeline")
    public ResponseEntity<BaseResponse> campaignPipeline(@AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(aggregateService.campaignPipeline(user.getIdx())));
    }

    /** ⭐NEW: Zone2 — 내 캠페인 진척률 랭킹 (완료율 내림차순). */
    @GetMapping("/campaign-progress")
    public ResponseEntity<BaseResponse> campaignProgress(@AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(aggregateService.campaignProgress(user.getIdx())));
    }

    /** ⭐NEW: Zone4 P2 — REVENUE KPI 월별 매출 추이 (최근 6개월). */
    @GetMapping("/revenue-trend")
    public ResponseEntity<BaseResponse> revenueTrend(@AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(aggregateService.revenueTrend(user.getIdx())));
    }

    private void requireAuth(AuthUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 유저 정보가 없습니다.");
        }
    }
}
