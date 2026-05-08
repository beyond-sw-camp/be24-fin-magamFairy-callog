package org.example.backend.campaign.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignMemberDto;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.campaign.service.CampaignMemberService;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/campaigns/{campaignId}/members")
@RequiredArgsConstructor
public class CampaignMemberController {

    private final CampaignMemberService memberService;
    private final CampaignRepository campaignRepository;

    @GetMapping
    public ResponseEntity<?> list(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user) {
        CampaignMemberDto.ListRes result = memberService.listMembers(toIdx(campaignId), user.getId());
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @GetMapping("/participants")
    public ResponseEntity<?> listParticipants(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user) {
        return ResponseEntity.ok(BaseResponse.success(memberService.listParticipants(toIdx(campaignId))));
    }

    @GetMapping("/candidates/team")
    public ResponseEntity<?> teamCandidates(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user) {
        List<CampaignMemberDto.CandidateRes> result = memberService.listTeamCandidates(toIdx(campaignId), user.getId());
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @GetMapping("/candidates/partner-gm")
    public ResponseEntity<?> partnerGmCandidates(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user) {
        List<CampaignMemberDto.CandidateRes> result = memberService.listPartnerGmCandidates(toIdx(campaignId), user.getId());
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PostMapping
    public ResponseEntity<?> addTeamMembers(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody CampaignMemberDto.AddTeamReq dto) {
        List<CampaignMemberDto.Res> result = memberService.addTeamMembers(toIdx(campaignId), user.getId(), dto.userIdxList());
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PostMapping("/invite-partner")
    public ResponseEntity<?> invitePartner(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody CampaignMemberDto.InvitePartnerReq dto) {
        CampaignMemberDto.InvitationRes result = memberService.invitePartnerGm(toIdx(campaignId), user.getId(), dto.userIdx());
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PostMapping("/invitations")
    public ResponseEntity<?> createInvitation(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody CampaignMemberDto.InvitePartnerReq dto) {
        CampaignMemberDto.InvitationRes result = memberService.invitePartnerGm(toIdx(campaignId), user.getId(), dto.userIdx());
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PatchMapping("/invitations/{invitationId}/accept")
    public ResponseEntity<?> acceptInvitation(
            @PathVariable String campaignId,
            @PathVariable Long invitationId,
            @AuthenticationPrincipal AuthUserDetails user) {
        CampaignMemberDto.InvitationRes result = memberService.acceptInvitation(toIdx(campaignId), invitationId, user.getId());
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PatchMapping("/invitations/{invitationId}/reject")
    public ResponseEntity<?> rejectInvitation(
            @PathVariable String campaignId,
            @PathVariable Long invitationId,
            @AuthenticationPrincipal AuthUserDetails user) {
        CampaignMemberDto.InvitationRes result = memberService.rejectInvitation(toIdx(campaignId), invitationId, user.getId());
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<?> updateRole(
            @PathVariable String campaignId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody CampaignMemberDto.UpdateRoleReq dto) {
        CampaignMemberDto.Res result = memberService.updateMemberRole(toIdx(campaignId), user.getId(), memberId, dto.campaignRole());
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<?> remove(
            @PathVariable String campaignId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal AuthUserDetails user) {
        memberService.removeMember(toIdx(campaignId), user.getId(), memberId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    private Long toIdx(String publicId) {
        return campaignRepository.findByPublicId(publicId)
                .map(Campaign::getIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    }
}
