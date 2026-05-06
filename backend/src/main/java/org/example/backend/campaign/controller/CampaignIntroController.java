package org.example.backend.campaign.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.CampaignIntroDto;
import org.example.backend.campaign.service.CampaignIntroService;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.model.BaseResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/campaigns")
@RequiredArgsConstructor
public class CampaignIntroController {

    private final CampaignIntroService introService;

    @GetMapping("/{campaignId}/intro")
    public ResponseEntity getIntro(@PathVariable Long campaignId) {
        try {
            CampaignIntroDto.GetRes dto = introService.getIntro(campaignId);
            return ResponseEntity.ok(BaseResponse.success(dto));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(BaseResponseStatus.NO_SUCH_ELEMENT));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        }
    }

    // ⚠️ 권한 검사 없음 (사용자 명시 지시 — 추후 추가 필요)
    @PatchMapping("/{campaignId}/intro")
    public ResponseEntity updateIntro(
            @PathVariable Long campaignId,
            @RequestBody CampaignIntroDto.UpdateReq dto
    ) {
        try {
            introService.updateIntro(campaignId, dto);
            return ResponseEntity.ok(BaseResponse.success(BaseResponseStatus.SUCCESS));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(BaseResponseStatus.NO_SUCH_ELEMENT));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        }
    }
}
