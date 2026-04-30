package org.example.backend.teamboard.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.security.RoleGuard;
import org.example.backend.teamboard.model.TaskPartsDto;
import org.example.backend.teamboard.service.TaskPartsService;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TaskPartsController {

    private final TaskPartsService taskPartsService;

    /** 캠페인 종속 - 업무 파트 목록 (모든 인증 사용자) */
    @GetMapping("/campaigns/{campaignId}/task-parts")
    public ResponseEntity<BaseResponse> list(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(
                taskPartsService.listByCampaign(campaignId)
        ));
    }

    /** 단건 상세 (모든 인증 사용자) */
    @GetMapping("/task-parts/{taskPartId}")
    public ResponseEntity<BaseResponse> getOne(
            @PathVariable Long taskPartId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(
                taskPartsService.getOne(taskPartId)
        ));
    }

    /** 업무 파트 생성 (ROLE_MANAGER) - 마일스톤 종속 */
    @PostMapping("/campaigns/{campaignId}/task-parts")
    public ResponseEntity<BaseResponse> create(
            @PathVariable Long campaignId,
            @RequestParam Long milestoneId,
            @RequestBody TaskPartsDto.ReqTaskParts req,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireManager(user);
        return ResponseEntity.ok(BaseResponse.success(
                taskPartsService.create(campaignId, milestoneId, req)
        ));
    }

    /** 업무 파트 수정 (ROLE_MANAGER) */
    @PutMapping("/task-parts/{taskPartId}")
    public ResponseEntity<BaseResponse> update(
            @PathVariable Long taskPartId,
            @RequestParam(required = false) Long milestoneId,
            @RequestBody TaskPartsDto.ReqTaskParts req,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireManager(user);
        return ResponseEntity.ok(BaseResponse.success(
                taskPartsService.update(taskPartId, milestoneId, req)
        ));
    }

    /** 업무 파트 삭제 (ROLE_MANAGER) */
    @DeleteMapping("/task-parts/{taskPartId}")
    public ResponseEntity<BaseResponse> delete(
            @PathVariable Long taskPartId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireManager(user);
        taskPartsService.delete(taskPartId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
