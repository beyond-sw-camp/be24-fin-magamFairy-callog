package org.example.backend.campaign.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.CampaignDto;
import org.example.backend.campaign.service.CampaignService;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<BaseResponse> listCampaigns(@AuthenticationPrincipal AuthUserDetails user) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.listCampaigns(currentUser(user))
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

    private String currentUser(AuthUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유저 정보가 없습니다.");
        }
        // 이곳에 한화랑 한화의 그룹사의 권한이 아니면 에러 터지는 로직 짜기

        return user.getId();
    }
}
