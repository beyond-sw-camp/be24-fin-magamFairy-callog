package org.example.backend.adcheck.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.adcheck.model.AdCheckDto;
import org.example.backend.adcheck.service.AdReviewRequestService;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/campaigns/{campaignId}/ad-review-requests")
public class AdReviewRequestController {

    private final AdReviewRequestService adReviewRequestService;
    private final CampaignRepository campaignRepository;

    @GetMapping
    public ResponseEntity<BaseResponse<?>> list(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(adReviewRequestService.list(toIdx(campaignId), user)));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<?>> create(
            @PathVariable String campaignId,
            @RequestBody AdCheckDto.ReviewRequestCreateReq req,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(adReviewRequestService.create(toIdx(campaignId), req, user)));
    }

    @PatchMapping("/{requestId}/approve")
    public ResponseEntity<BaseResponse<?>> approve(
            @PathVariable String campaignId,
            @PathVariable Long requestId,
            @RequestBody(required = false) AdCheckDto.ReviewDecisionReq req,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(adReviewRequestService.approve(toIdx(campaignId), requestId, req, user)));
    }

    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<BaseResponse<?>> reject(
            @PathVariable String campaignId,
            @PathVariable Long requestId,
            @RequestBody(required = false) AdCheckDto.ReviewDecisionReq req,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(adReviewRequestService.reject(toIdx(campaignId), requestId, req, user)));
    }

    private Long toIdx(String publicId) {
        return campaignRepository.findByPublicId(publicId)
                .map(Campaign::getIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found."));
    }
}
