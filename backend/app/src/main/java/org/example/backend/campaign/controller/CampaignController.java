package org.example.backend.campaign.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignDto;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.campaign.service.CampaignCalendarService;
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
    private final CampaignCalendarService campaignCalendarService;
    private final CampaignRepository campaignRepository;

    @GetMapping
    public ResponseEntity<BaseResponse> listCampaigns(
            @AuthenticationPrincipal AuthUserDetails user,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "mine") String scope
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.listCampaigns(user.getIdx(), scope)
        ));
    }

    /** 캘린더 일괄 조회 — 캠페인 + 모집마감 + 마일스톤을 1회 호출로 모두 반환. */
    @GetMapping("/calendar-events")
    public ResponseEntity<BaseResponse> calendarEvents(
            @AuthenticationPrincipal AuthUserDetails user,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "mine") String scope
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignCalendarService.loadEvents(user.getIdx(), scope)
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
            @PathVariable String campaignId,
            @RequestBody CampaignDto.UpsertReq dto,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.updateCampaign(currentUser(user), toIdx(campaignId), dto)
        ));
    }

    @PatchMapping("/{campaignId}/status")
    public ResponseEntity<BaseResponse> updateCampaignStatus(
            @PathVariable String campaignId,
            @RequestBody CampaignDto.StatusReq dto,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.updateStatus(currentUser(user), toIdx(campaignId), dto)
        ));
    }

    @PostMapping("/{campaignId}/partners/invitations")
    public ResponseEntity<BaseResponse> inviteCampaignPartners(
            @PathVariable String campaignId,
            @RequestBody CampaignDto.PartnerInviteReq dto,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.invitePartners(currentUser(user), toIdx(campaignId), dto)
        ));
    }

    @PostMapping("/{campaignId}/thumbnail/upload-url")
    public ResponseEntity<BaseResponse> createThumbnailUploadUrl(
            @PathVariable String campaignId,
            @RequestBody ThumbnailUploadReq dto,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.createThumbnailUploadUrl(currentUser(user), toIdx(campaignId),
                        dto == null ? null : dto.contentType(),
                        dto == null ? null : dto.fileSize())
        ));
    }

    @PatchMapping("/{campaignId}/thumbnail")
    public ResponseEntity<BaseResponse> confirmThumbnail(
            @PathVariable String campaignId,
            @RequestBody ThumbnailConfirmReq dto,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        campaignService.confirmThumbnail(currentUser(user), toIdx(campaignId), dto == null ? null : dto.objectKey());
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @DeleteMapping("/{campaignId}/thumbnail")
    public ResponseEntity<BaseResponse> clearThumbnail(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        campaignService.clearThumbnail(currentUser(user), toIdx(campaignId));
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @PostMapping("/{campaignId}/thumbnail/generate")
    public ResponseEntity<BaseResponse> regenerateThumbnail(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        campaignService.regenerateThumbnail(currentUser(user), toIdx(campaignId));
        return ResponseEntity.ok(BaseResponse.success("썸네일 생성을 시작했습니다."));
    }

    public record ThumbnailUploadReq(String contentType, Long fileSize) {}
    public record ThumbnailConfirmReq(String objectKey) {}

    private String currentUser(AuthUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유저 정보가 없습니다.");
        }
        return user.getId();
    }

    private Long toIdx(String publicId) {
        return campaignRepository.findByPublicId(publicId)
                .map(Campaign::getIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    }
}
