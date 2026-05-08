package org.example.backend.campaign.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.CampaignIntroDto;
import org.example.backend.campaign.service.CampaignIntroService;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.model.BaseResponseStatus;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/campaigns")
@RequiredArgsConstructor
public class CampaignIntroController {

    private final CampaignIntroService introService;

    @GetMapping("/{campaignId}/intro")
    public ResponseEntity getIntro(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        try {
            Long callerIdx = user == null ? null : user.getIdx();
            CampaignIntroDto.GetRes dto = introService.getIntro(campaignId, callerIdx);
            return ResponseEntity.ok(BaseResponse.success(dto));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(BaseResponseStatus.NO_SUCH_ELEMENT));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        }
    }

    @PatchMapping("/{campaignId}/intro")
    public ResponseEntity updateIntro(
            @PathVariable Long campaignId,
            @RequestBody CampaignIntroDto.UpdateReq dto,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        try {
            introService.updateIntro(campaignId, dto, user.getIdx());
            return ResponseEntity.ok(BaseResponse.success(BaseResponseStatus.SUCCESS));
        } catch (ResponseStatusException e) {
            // 권한 거부(403) 등은 그대로 위로 전파
            throw e;
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(BaseResponseStatus.NO_SUCH_ELEMENT));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        }
    }
}
