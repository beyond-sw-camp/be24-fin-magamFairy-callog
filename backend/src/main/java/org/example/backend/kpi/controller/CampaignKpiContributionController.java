package org.example.backend.kpi.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.repository.CampaignRepository;
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
    private final CampaignRepository campaignRepository;

    @GetMapping
    public ResponseEntity<BaseResponse> list(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(contributionService.list(toIdx(campaignId))));
    }

    @PostMapping
    public ResponseEntity<BaseResponse> create(
            @PathVariable String campaignId,
            @RequestBody CreateContributionRequest req,
            @AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(
                contributionService.create(user.getIdx(), toIdx(campaignId), req)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BaseResponse> update(
            @PathVariable String campaignId,
            @PathVariable Long id,
            @RequestBody UpdateContributionReq req,
            @AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(
                contributionService.update(user.getIdx(), toIdx(campaignId), id,
                        req == null ? null : req.committedValue(),
                        req == null ? null : req.actualValue())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse> delete(
            @PathVariable String campaignId,
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        contributionService.delete(user.getIdx(), toIdx(campaignId), id);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    public record UpdateContributionReq(BigDecimal committedValue, BigDecimal actualValue) {}

    private void requireAuth(AuthUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 유저 정보가 없습니다.");
        }
    }

    private Long toIdx(String publicId) {
        return campaignRepository.findByPublicId(publicId)
                .map(Campaign::getIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    }
}
