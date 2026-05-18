package org.example.backend.campaignframe.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaignframe.model.CampaignFrameDto;
import org.example.backend.campaignframe.service.CampaignFrameService;
import org.example.backend.common.model.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/frames")
@RequiredArgsConstructor
public class CampaignFrameController {
    private final CampaignFrameService campaignFrameService;

    @PostMapping("/create")
    public ResponseEntity<?> createFrame(
            @RequestBody CampaignFrameDto.CreateFrameReq dto,
            Authentication authentication
    ) {
        CampaignFrameDto.FrameRes result = campaignFrameService.createFrame(dto, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(result));
    }

    @GetMapping("/list")
    public ResponseEntity<?> getFrameList(Authentication authentication) {
        return ResponseEntity.ok(BaseResponse.success(campaignFrameService.getFrameList(authentication)));
    }

    @GetMapping("/detail/{frameId}")
    public ResponseEntity<?> getFrame(
            @PathVariable String frameId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(BaseResponse.success(campaignFrameService.getFrame(frameId, authentication)));
    }

    @PutMapping("/update/{frameId}")
    public ResponseEntity<?> updateFrame(
            @PathVariable String frameId,
            @RequestBody CampaignFrameDto.UpdateFrameReq dto,
            Authentication authentication
    ) {
        CampaignFrameDto.FrameRes result = campaignFrameService.updateFrame(frameId, dto, authentication);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @DeleteMapping("/delete/{frameId}")
    public ResponseEntity<?> deleteFrame(
            @PathVariable String frameId,
            Authentication authentication
    ) {
        CampaignFrameDto.DeleteFrameRes result = campaignFrameService.deleteFrame(frameId, authentication);
        return ResponseEntity.ok(BaseResponse.success(result));
    }
}
