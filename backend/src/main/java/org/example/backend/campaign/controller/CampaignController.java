package org.example.backend.campaign.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.CampaignDto;
import org.example.backend.campaign.service.CampaignService;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/campaigns")
public class CampaignController {
    private final CampaignService campaignService;

    @GetMapping
    public ResponseEntity<BaseResponse> listCampaigns(
            @AuthenticationPrincipal AuthUserDetails user,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "mine") String scope
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.listCampaigns(user.getIdx(), scope)
        ));
    }

    @GetMapping("/directory")
    public ResponseEntity<BaseResponse> directory(
            @AuthenticationPrincipal AuthUserDetails user,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String q,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String orgType,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String status,
            @org.springframework.web.bind.annotation.RequestParam(required = false) java.util.List<String> tags,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "latest") String sort,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String scope
    ) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.directory(user.getIdx(), q, orgType, status, tags, sort, scope)
        ));
    }

    @PostMapping("/new")
    public ResponseEntity<BaseResponse> createCampaign(
            @RequestBody CampaignDto.UpsertReq dto,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.createCampaign(currentUser(user), dto)
        ));
    }

    @PutMapping("/{campaignId}")
    public ResponseEntity<BaseResponse> updateCampaign(
            @PathVariable Long campaignId,
            @RequestBody CampaignDto.UpsertReq dto,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.updateCampaign(currentUser(user), campaignId, dto)
        ));
    }

    @PatchMapping("/{campaignId}/status")
    public ResponseEntity<BaseResponse> updateCampaignStatus(
            @PathVariable Long campaignId,
            @RequestBody CampaignDto.StatusReq dto,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.updateStatus(currentUser(user), campaignId, dto)
        ));
    }

    @PostMapping("/{campaignId}/partners/invitations")
    public ResponseEntity<BaseResponse> inviteCampaignPartners(
            @PathVariable Long campaignId,
            @RequestBody CampaignDto.PartnerInviteReq dto,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.invitePartners(currentUser(user), campaignId, dto)
        ));
    }

    /** 썸네일 업로드 — presigned PUT URL 발급 (Phase 3). */
    @PostMapping("/{campaignId}/thumbnail/upload-url")
    public ResponseEntity<BaseResponse> createThumbnailUploadUrl(
            @PathVariable Long campaignId,
            @RequestBody ThumbnailUploadReq dto,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.createThumbnailUploadUrl(currentUser(user), campaignId,
                        dto == null ? null : dto.contentType(),
                        dto == null ? null : dto.fileSize())
        ));
    }

    /** 썸네일 업로드 확정 — objectKey 저장. */
    @PatchMapping("/{campaignId}/thumbnail")
    public ResponseEntity<BaseResponse> confirmThumbnail(
            @PathVariable Long campaignId,
            @RequestBody ThumbnailConfirmReq dto,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        campaignService.confirmThumbnail(currentUser(user), campaignId, dto == null ? null : dto.objectKey());
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    /** 썸네일 삭제. */
    @DeleteMapping("/{campaignId}/thumbnail")
    public ResponseEntity<BaseResponse> clearThumbnail(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        campaignService.clearThumbnail(currentUser(user), campaignId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    /** 썸네일 AI 자동 재생성 (Phase 4) — 비동기 처리, 응답은 즉시 반환. */
    @PostMapping("/{campaignId}/thumbnail/generate")
    public ResponseEntity<BaseResponse> regenerateThumbnail(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        campaignService.regenerateThumbnail(currentUser(user), campaignId);
        return ResponseEntity.ok(BaseResponse.success("썸네일 생성을 시작했습니다."));
    }

    public record ThumbnailUploadReq(String contentType, Long fileSize) {}
    public record ThumbnailConfirmReq(String objectKey) {}

    private String currentUser(AuthUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유저 정보가 없습니다.");
        }
        // 이곳에 한화랑 한화의 그룹사의 권한이 아니면 에러 터지는 로직 짜기

        return user.getId();
    }
}
