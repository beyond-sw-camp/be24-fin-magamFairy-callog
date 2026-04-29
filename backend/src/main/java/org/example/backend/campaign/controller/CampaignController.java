package org.example.backend.campaign.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.CampaignDto;
import org.example.backend.campaign.service.CampaignService;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/campaigns")
public class CampaignController {
    private final CampaignService campaignService;

    @GetMapping
    public ResponseEntity<BaseResponse> listCampaigns(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String keyword,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.getCampaigns(currentUser(user), status, dateFrom, dateTo, tags, keyword)
        ));
    }

    @PostMapping("/new")
    public ResponseEntity<BaseResponse> createCampaign(
            @RequestBody CampaignDto.UpsertReq dto,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.createCampaign(currentUser(user), dto)
        ));
    }

    @PutMapping("/{campaignId}")
    public ResponseEntity<BaseResponse> updateCampaign(
            @PathVariable Long campaignId,
            @RequestBody CampaignDto.UpsertReq dto,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.updateCampaign(currentUser(user), campaignId, dto)
        ));
    }

    @PatchMapping("/{campaignId}/status")
    public ResponseEntity<BaseResponse> updateCampaignStatus(
            @PathVariable Long campaignId,
            @RequestBody CampaignDto.StatusReq dto,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.updateStatus(currentUser(user), campaignId, dto)
        ));
    }

    @PostMapping("/{campaignId}/partners/invitations")
    public ResponseEntity<BaseResponse> inviteCampaignPartners(
            @PathVariable Long campaignId,
            @RequestBody CampaignDto.PartnerInviteReq dto,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.invitePartners(currentUser(user), campaignId, dto)
        ));
    }

    @GetMapping("/{campaignId}/invitations")
    public ResponseEntity<BaseResponse> listInvitations(
            @PathVariable Long campaignId,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.getInvitations(currentUser(user), campaignId)
        ));
    }

    @PatchMapping("/{campaignId}/invitations/{invitationId}/accept")
    public ResponseEntity<BaseResponse> acceptInvitation(
            @PathVariable Long campaignId,
            @PathVariable Long invitationId,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.acceptInvitation(currentUser(user), campaignId, invitationId)
        ));
    }

    @PatchMapping("/{campaignId}/invitations/{invitationId}/reject")
    public ResponseEntity<BaseResponse> rejectInvitation(
            @PathVariable Long campaignId,
            @PathVariable Long invitationId,
            @RequestBody CampaignDto.InvitationDecisionReq dto,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.rejectInvitation(currentUser(user), campaignId, invitationId, dto)
        ));
    }

    @DeleteMapping("/{campaignId}/invitations/{invitationId}")
    public ResponseEntity<BaseResponse> cancelInvitation(
            @PathVariable Long campaignId,
            @PathVariable Long invitationId,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.cancelInvitation(currentUser(user), campaignId, invitationId)
        ));
    }

    @GetMapping("/{campaignId}/participants")
    public ResponseEntity<BaseResponse> listParticipants(
            @PathVariable Long campaignId,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.getParticipants(currentUser(user), campaignId)
        ));
    }

    @PatchMapping("/{campaignId}/participants/{participantId}/role")
    public ResponseEntity<BaseResponse> updateParticipantRole(
            @PathVariable Long campaignId,
            @PathVariable Long participantId,
            @RequestBody CampaignDto.RoleUpdateReq dto,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.updateParticipantRole(currentUser(user), campaignId, participantId, dto)
        ));
    }

    @PostMapping("/{campaignId}/approval-requests")
    public ResponseEntity<BaseResponse> requestApproval(
            @PathVariable Long campaignId,
            @RequestBody CampaignDto.ApprovalRequestReq dto,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.requestApproval(currentUser(user), campaignId, dto)
        ));
    }

    @GetMapping("/{campaignId}/approval-requests")
    public ResponseEntity<BaseResponse> listApprovals(
            @PathVariable Long campaignId,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.getApprovals(currentUser(user), campaignId)
        ));
    }

    @PatchMapping("/{campaignId}/approval-requests/{approvalId}/approve")
    public ResponseEntity<BaseResponse> approveApproval(
            @PathVariable Long campaignId,
            @PathVariable Long approvalId,
            @RequestBody CampaignDto.ApprovalDecisionReq dto,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.approveApproval(currentUser(user), campaignId, approvalId, dto)
        ));
    }

    @PatchMapping("/{campaignId}/approval-requests/{approvalId}/reject")
    public ResponseEntity<BaseResponse> rejectApproval(
            @PathVariable Long campaignId,
            @PathVariable Long approvalId,
            @RequestBody CampaignDto.ApprovalDecisionReq dto,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.rejectApproval(currentUser(user), campaignId, approvalId, dto)
        ));
    }

    @GetMapping("/{campaignId}/comments")
    public ResponseEntity<BaseResponse> listComments(
            @PathVariable Long campaignId,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.getComments(currentUser(user), campaignId)
        ));
    }

    @PostMapping("/{campaignId}/comments")
    public ResponseEntity<BaseResponse> addComment(
            @PathVariable Long campaignId,
            @RequestBody CampaignDto.CommentReq dto,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.addComment(currentUser(user), campaignId, dto)
        ));
    }

    @PatchMapping("/{campaignId}/comments/{commentId}/resolve")
    public ResponseEntity<BaseResponse> resolveComment(
            @PathVariable Long campaignId,
            @PathVariable Long commentId,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.resolveComment(currentUser(user), campaignId, commentId)
        ));
    }

    @GetMapping("/{campaignId}/versions")
    public ResponseEntity<BaseResponse> listVersions(
            @PathVariable Long campaignId,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.getVersions(currentUser(user), campaignId)
        ));
    }

    @GetMapping("/{campaignId}/analytics")
    public ResponseEntity<BaseResponse> getAnalytics(
            @PathVariable Long campaignId,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.getAnalytics(currentUser(user), campaignId)
        ));
    }

    @PutMapping("/{campaignId}/analytics")
    public ResponseEntity<BaseResponse> updateAnalytics(
            @PathVariable Long campaignId,
            @RequestBody CampaignDto.AnalyticsReq dto,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.updateAnalytics(currentUser(user), campaignId, dto)
        ));
    }

    @PostMapping("/{campaignId}/clone")
    public ResponseEntity<BaseResponse> cloneCampaign(
            @PathVariable Long campaignId,
            @RequestBody CampaignDto.CloneReq dto,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.cloneCampaign(currentUser(user), campaignId, dto)
        ));
    }

    @GetMapping("/{campaignId}/contents")
    public ResponseEntity<BaseResponse> listContents(
            @PathVariable Long campaignId,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.getContents(currentUser(user), campaignId)
        ));
    }

    @PostMapping("/{campaignId}/contents")
    public ResponseEntity<BaseResponse> addContent(
            @PathVariable Long campaignId,
            @RequestBody CampaignDto.ContentReq dto,
            AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                campaignService.addContent(currentUser(user), campaignId, dto)
        ));
    }

    private String currentUser(AuthUserDetails user) {
        if (user == null || !user.isAccountNonExpired()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AuthUserDetails is required.");
        }

        return user.getName();
    }
}
