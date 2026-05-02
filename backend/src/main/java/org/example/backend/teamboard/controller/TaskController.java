package org.example.backend.teamboard.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.security.RoleGuard;
import org.example.backend.teamboard.model.TaskDto;
import org.example.backend.teamboard.service.TaskService;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /** 메인 팀 보드 - 내가 참여한 캠페인의 Task (모든 인증 사용자) */
    @GetMapping("/tasks")
    public ResponseEntity<BaseResponse> listAll(
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(
                taskService.listAll(user.getIdx())
        ));
    }

    /** 캠페인 팀 보드 - 캠페인 종속 Task (모든 인증 사용자) */
    @GetMapping("/campaigns/{campaignId}/tasks")
    public ResponseEntity<BaseResponse> listByCampaign(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(
                taskService.listByCampaign(campaignId)
        ));
    }

    /** Task 단건 상세 (모든 인증 사용자) */
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<BaseResponse> getOne(
            @PathVariable Long taskId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(
                taskService.getOne(taskId)
        ));
    }

    /** Task 생성 (ROLE_MANAGER) */
    @PostMapping("/campaigns/{campaignId}/tasks")
    public ResponseEntity<BaseResponse> create(
            @PathVariable Long campaignId,
            @RequestBody TaskDto.ReqTask req,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireManager(user);
        return ResponseEntity.ok(BaseResponse.success(
                taskService.create(campaignId, req)
        ));
    }

    /** Task 수정 (ROLE_MANAGER) */
    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<BaseResponse> update(
            @PathVariable Long taskId,
            @RequestBody TaskDto.ReqTask req,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireManager(user);
        return ResponseEntity.ok(BaseResponse.success(
                taskService.update(taskId, req)
        ));
    }

    /** Task 삭제 (ROLE_MANAGER) */
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<BaseResponse> delete(
            @PathVariable Long taskId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireManager(user);
        taskService.delete(taskId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
