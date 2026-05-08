package org.example.backend.teamboard.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.security.RoleGuard;
import org.example.backend.teamboard.model.TaskDto;
import org.example.backend.teamboard.service.TaskService;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final CampaignRepository campaignRepository;

    @GetMapping("/tasks")
    public ResponseEntity<BaseResponse> listAll(
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(
                taskService.listAll(user.getIdx())
        ));
    }

    @GetMapping("/campaigns/{campaignId}/tasks")
    public ResponseEntity<BaseResponse> listByCampaign(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(
                taskService.listByCampaign(toIdx(campaignId))
        ));
    }

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

    @PostMapping("/campaigns/{campaignId}/tasks")
    public ResponseEntity<BaseResponse> create(
            @PathVariable String campaignId,
            @RequestBody TaskDto.ReqTask req,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireManager(user);
        return ResponseEntity.ok(BaseResponse.success(
                taskService.create(campaignId, req, user)
        ));
    }

    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<BaseResponse> update(
            @PathVariable Long taskId,
            @RequestBody TaskDto.ReqTask req,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireManager(user);
        return ResponseEntity.ok(BaseResponse.success(
                taskService.update(taskId, req, user)
        ));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<BaseResponse> delete(
            @PathVariable Long taskId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireManager(user);
        taskService.delete(taskId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    private Long toIdx(String publicId) {
        return campaignRepository.findByPublicId(publicId)
                .map(Campaign::getIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    }
}
