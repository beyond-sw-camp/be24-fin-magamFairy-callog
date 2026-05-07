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

    private void requireAuth(AuthUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 유저 정보가 없습니다.");
        }
    }
}
