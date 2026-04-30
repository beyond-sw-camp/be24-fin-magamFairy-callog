package org.example.backend.teamboard.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.security.RoleGuard;
import org.example.backend.teamboard.model.MileStonesDto;
import org.example.backend.teamboard.service.MileStonesService;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MileStonesController {

    private final MileStonesService mileStonesService;

    /** 캠페인 종속 - 마일스톤 목록 (모든 인증 사용자) */
    @GetMapping("/campaigns/{campaignId}/milestones")
    public ResponseEntity<BaseResponse> list(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(
                mileStonesService.listByCampaign(campaignId)
        ));
    }

    /** 단건 상세 (모든 인증 사용자) */
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

    /** 마일스톤 생성 (ROLE_MANAGER) */
    @PostMapping("/campaigns/{campaignId}/milestones")
    public ResponseEntity<BaseResponse> create(
            @PathVariable Long campaignId,
            @RequestBody MileStonesDto.ReqMileStones req,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireManager(user);
        return ResponseEntity.ok(BaseResponse.success(
                mileStonesService.create(campaignId, req)
        ));
    }

    /** 마일스톤 수정 (ROLE_MANAGER) */
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

    /** 마일스톤 삭제 (ROLE_MANAGER) */
    @DeleteMapping("/milestones/{milestoneId}")
    public ResponseEntity<BaseResponse> delete(
            @PathVariable Long milestoneId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireManager(user);
        mileStonesService.delete(milestoneId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
