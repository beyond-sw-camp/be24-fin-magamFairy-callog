package org.example.backend.campaign.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.CampaignProposalDto;
import org.example.backend.campaign.service.CampaignProposalService;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.model.BaseResponseStatus;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/campaigns")
@RequiredArgsConstructor
public class CampaignProposalController {

    private final CampaignProposalService proposalService;

    @PostMapping("/{campaignId}/proposals")
    public ResponseEntity submitProposal(
            @PathVariable Long campaignId,
            @RequestBody CampaignProposalDto.SubmitReq dto,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        try {
            CampaignProposalDto.SubmitRes res = proposalService.submitProposal(campaignId, dto, user);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(BaseResponse.success(BaseResponseStatus.SUCCESS, res));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(BaseResponseStatus.NO_SUCH_ELEMENT));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        }
    }
}
