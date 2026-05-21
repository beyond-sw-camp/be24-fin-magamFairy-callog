package org.example.backend.teamboard.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.security.RoleGuard;
import org.example.backend.teamboard.model.MileStonesDto;
import org.example.backend.teamboard.service.MileStonesService;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class MileStonesController {

    private final MileStonesService mileStonesService;
    private final CampaignRepository campaignRepository;

    @GetMapping("/campaigns/{campaignId}/milestones")
    public ResponseEntity<BaseResponse> list(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(
                mileStonesService.listByCampaign(toIdx(campaignId))
        ));
    }

    @GetMapping("/milestones/{milestoneId}")
    public ResponseEntity<BaseResponse> getOne(
            @PathVariable Long milestoneId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(
                mileStonesService.getOne(milestoneId)
        ));
    }

    @PostMapping("/campaigns/{campaignId}/milestones")
    public ResponseEntity<BaseResponse> create(
            @PathVariable String campaignId,
            @RequestBody MileStonesDto.ReqMileStones req,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireManager(user);
        return ResponseEntity.ok(BaseResponse.success(
                mileStonesService.create(toIdx(campaignId), req)
        ));
    }

    @PutMapping("/milestones/{milestoneId}")
    public ResponseEntity<BaseResponse> update(
            @PathVariable Long milestoneId,
            @RequestBody MileStonesDto.ReqMileStones req,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireManager(user);
        return ResponseEntity.ok(BaseResponse.success(
                mileStonesService.update(milestoneId, req)
        ));
    }

    @DeleteMapping("/milestones/{milestoneId}")
    public ResponseEntity<BaseResponse> delete(
            @PathVariable Long milestoneId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireManager(user);
        mileStonesService.delete(milestoneId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    private Long toIdx(String campaignId) {
        return campaignRepository.findByPublicId(campaignId)
                .map(Campaign::getIdx)
                // publicId가 없는(legacy) 캠페인은 프론트가 idx 문자열을 보냄 → 숫자면 idx로 폴백 조회
                .orElseGet(() -> {
                    if (campaignId != null && campaignId.matches("\\d+")
                            && campaignRepository.existsById(Long.parseLong(campaignId))) {
                        return Long.parseLong(campaignId);
                    }
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다.");
                });
    }
}
