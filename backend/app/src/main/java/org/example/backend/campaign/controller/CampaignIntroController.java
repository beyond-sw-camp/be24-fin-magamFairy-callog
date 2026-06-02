package org.example.backend.campaign.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignIntroDto;
import org.example.backend.campaign.repository.CampaignRepository;
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
    private final CampaignRepository campaignRepository;

    @GetMapping("/{campaignId}/intro")
    public ResponseEntity getIntro(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        Long campaignIdx = toIdx(campaignId);
        Long callerIdx = user == null ? null : user.getIdx();
        CampaignIntroDto.GetRes dto = introService.getIntro(campaignIdx, callerIdx);
        return ResponseEntity.ok(BaseResponse.success(dto));
    }

    @PatchMapping("/{campaignId}/intro")
    public ResponseEntity updateIntro(
            @PathVariable String campaignId,
            @RequestBody CampaignIntroDto.UpdateReq dto,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Long campaignIdx = toIdx(campaignId);
        introService.updateIntro(campaignIdx, dto, user.getIdx());
        return ResponseEntity.ok(BaseResponse.success(BaseResponseStatus.SUCCESS));
    }

    private Long toIdx(String publicId) {
        return campaignRepository.findByPublicId(publicId)
                .map(Campaign::getIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    }
}
