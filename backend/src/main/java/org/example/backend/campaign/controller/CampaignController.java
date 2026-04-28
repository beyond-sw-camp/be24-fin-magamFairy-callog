package org.example.backend.campaign.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.CampaignDto;
import org.example.backend.campaign.service.CampaignService;
import org.example.backend.common.model.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<BaseResponse> listCampaigns(Authentication authentication) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.getCampaigns(currentUser(authentication))
        ));
    }

    @PostMapping("/new")
    public ResponseEntity<BaseResponse> createCampaign(
            @RequestBody CampaignDto.UpsertReq dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.createCampaign(currentUser(authentication), dto)
        ));
    }

    @PutMapping("/{campaignId}")
    public ResponseEntity<BaseResponse> updateCampaign(
            @PathVariable Long campaignId,
            @RequestBody CampaignDto.UpsertReq dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.updateCampaign(currentUser(authentication), campaignId, dto)
        ));
    }

    @PatchMapping("/{campaignId}/status")
    public ResponseEntity<BaseResponse> updateCampaignStatus(
            @PathVariable Long campaignId,
            @RequestBody CampaignDto.StatusReq dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.updateStatus(currentUser(authentication), campaignId, dto)
        ));
    }

    @PostMapping("/{campaignId}/partners/invitations")
    public ResponseEntity<BaseResponse> inviteCampaignPartners(
            @PathVariable Long campaignId,
            @RequestBody CampaignDto.PartnerInviteReq dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.invitePartners(currentUser(authentication), campaignId, dto)
        ));
    }

    private String currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }

        return authentication.getName();
    }
}
