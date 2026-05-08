package org.example.backend.adcheck.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.adcheck.model.AdCheckDto;
import org.example.backend.adcheck.service.AdReviewRequestService;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/campaigns/{campaignId}/ad-review-requests")
public class AdReviewRequestController {

    private final AdReviewRequestService adReviewRequestService;

    @GetMapping
    public ResponseEntity<BaseResponse<?>> list(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(adReviewRequestService.list(campaignId, user)));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<?>> create(
            @PathVariable Long campaignId,
            @RequestBody AdCheckDto.ReviewRequestCreateReq req,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(adReviewRequestService.create(campaignId, req, user)));
    }

    @PatchMapping("/{requestId}/approve")
    public ResponseEntity<BaseResponse<?>> approve(
            @PathVariable Long campaignId,
            @PathVariable Long requestId,
            @RequestBody(required = false) AdCheckDto.ReviewDecisionReq req,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(adReviewRequestService.approve(campaignId, requestId, req, user)));
    }

    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<BaseResponse<?>> reject(
            @PathVariable Long campaignId,
            @PathVariable Long requestId,
            @RequestBody(required = false) AdCheckDto.ReviewDecisionReq req,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(adReviewRequestService.reject(campaignId, requestId, req, user)));
    }
}
