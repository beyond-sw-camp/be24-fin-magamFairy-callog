package org.example.backend.teamboard.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.security.RoleGuard;
import org.example.backend.teamboard.model.TaskPartsDto;
import org.example.backend.teamboard.service.TaskPartsService;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class TaskPartsController {

    private final TaskPartsService taskPartsService;
    private final CampaignRepository campaignRepository;

    @GetMapping("/campaigns/{campaignId}/task-parts")
    public ResponseEntity<BaseResponse> list(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(
                taskPartsService.listByCampaign(toIdx(campaignId))
        ));
    }

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

    @PostMapping("/campaigns/{campaignId}/task-parts")
    public ResponseEntity<BaseResponse> create(
            @PathVariable String campaignId,
            @RequestParam Long milestoneId,
            @RequestBody TaskPartsDto.ReqTaskParts req,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireManager(user);
        return ResponseEntity.ok(BaseResponse.success(
                taskPartsService.create(toIdx(campaignId), milestoneId, req)
        ));
    }

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

    @DeleteMapping("/task-parts/{taskPartId}")
    public ResponseEntity<BaseResponse> delete(
            @PathVariable Long taskPartId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireManager(user);
        taskPartsService.delete(taskPartId);
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
