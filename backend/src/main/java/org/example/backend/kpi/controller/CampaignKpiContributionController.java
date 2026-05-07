package org.example.backend.kpi.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.kpi.dto.CreateContributionRequest;
import org.example.backend.kpi.service.CampaignKpiContributionService;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/campaigns/{campaignId}/kpi-contributions")
public class CampaignKpiContributionController {

    private final CampaignKpiContributionService contributionService;

    @GetMapping
    public ResponseEntity<BaseResponse> list(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(contributionService.list(campaignId)));
    }

    @PostMapping
    public ResponseEntity<BaseResponse> create(
            @PathVariable Long campaignId,
            @RequestBody CreateContributionRequest req,
            @AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(
                contributionService.create(user.getIdx(), campaignId, req)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BaseResponse> update(
            @PathVariable Long campaignId,
            @PathVariable Long id,
            @RequestBody UpdateContributionReq req,
            @AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(
                contributionService.update(user.getIdx(), campaignId, id,
                        req == null ? null : req.committedValue(),
                        req == null ? null : req.actualValue())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse> delete(
            @PathVariable Long campaignId,
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        contributionService.delete(user.getIdx(), campaignId, id);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    public record UpdateContributionReq(BigDecimal committedValue, BigDecimal actualValue) {}

    private void requireAuth(AuthUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 유저 정보가 없습니다.");
        }
    }
}
