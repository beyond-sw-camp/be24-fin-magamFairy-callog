package org.example.backend.adcheck.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.adcheck.client.AiJudgeClient;
import org.example.backend.adcheck.service.AdAiAnalysisService;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.model.BaseResponseStatus;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/campaigns/{campaignId}/ad-analyses")
@RequiredArgsConstructor
@Slf4j
public class AdAiAnalysisController {
    private final AdAiAnalysisService adAiAnalysisService;

    @PostMapping(value = "/check/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<?>> checkFile(
            @PathVariable String campaignId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        try {
            return ResponseEntity.ok(BaseResponse.success(adAiAnalysisService.checkFile(campaignId, file, user)));
        } catch (AiJudgeClient.FileCheckRemoteException e) {
            log.error("Campaign AI analysis failed with partial response. campaignId={}, fileName={}",
                    campaignId, file == null ? null : file.getOriginalFilename(), e);
            return ResponseEntity.ok(BaseResponse.fail(BaseResponseStatus.FAIL, e.getResponse()));
        } catch (RuntimeException e) {
            log.error("Campaign AI analysis failed. campaignId={}, fileName={}",
                    campaignId, file == null ? null : file.getOriginalFilename(), e);
            return ResponseEntity.ok(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<BaseResponse<?>> list(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(adAiAnalysisService.list(campaignId, user)));
    }

    @GetMapping("/{analysisId}")
    public ResponseEntity<BaseResponse<?>> detail(
            @PathVariable String campaignId,
            @PathVariable Long analysisId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        return ResponseEntity.ok(BaseResponse.success(adAiAnalysisService.detail(campaignId, analysisId, user)));
    }
}
