package org.example.backend.campaign.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.CampaignMemberDto;
import org.example.backend.campaign.service.CampaignMemberService;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/campaigns/{campaignId}/members")
@RequiredArgsConstructor
public class CampaignMemberController {

    private final CampaignMemberService memberService;

    @GetMapping
    public ResponseEntity<?> list(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal AuthUserDetails user) {
        CampaignMemberDto.ListRes result = memberService.listMembers(campaignId, user.getId());
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @GetMapping("/participants")
    public ResponseEntity<?> listParticipants(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal AuthUserDetails user) {
        return ResponseEntity.ok(BaseResponse.success(memberService.listParticipants(campaignId)));
    }

    @GetMapping("/candidates/team")
    public ResponseEntity<?> teamCandidates(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal AuthUserDetails user) {
        List<CampaignMemberDto.CandidateRes> result = memberService.listTeamCandidates(campaignId, user.getId());
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @GetMapping("/candidates/partner-gm")
    public ResponseEntity<?> partnerGmCandidates(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal AuthUserDetails user) {
        List<CampaignMemberDto.CandidateRes> result = memberService.listPartnerGmCandidates(campaignId, user.getId());
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PostMapping
    public ResponseEntity<?> addTeamMembers(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody CampaignMemberDto.AddTeamReq dto) {
        List<CampaignMemberDto.Res> result = memberService.addTeamMembers(campaignId, user.getId(), dto.userIdxList());
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PostMapping("/invite-partner")
    public ResponseEntity<?> invitePartner(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody CampaignMemberDto.InvitePartnerReq dto) {
        CampaignMemberDto.Res result = memberService.invitePartnerGm(campaignId, user.getId(), dto.userIdx());
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<?> updateRole(
            @PathVariable Long campaignId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody CampaignMemberDto.UpdateRoleReq dto) {
        CampaignMemberDto.Res result = memberService.updateMemberRole(campaignId, user.getId(), memberId, dto.campaignRole());
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<?> remove(
            @PathVariable Long campaignId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal AuthUserDetails user) {
        memberService.removeMember(campaignId, user.getId(), memberId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
