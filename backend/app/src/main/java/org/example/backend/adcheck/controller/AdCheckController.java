package org.example.backend.adcheck.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.adcheck.model.AdCheckDto;
import org.example.backend.adcheck.service.AdCheckService;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.model.BaseResponseStatus;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ad")
@RequiredArgsConstructor
@Slf4j
public class AdCheckController {

    private final AdCheckService adCheckService;

    @PostMapping("/check")
    public ResponseEntity<BaseResponse<?>> check(@RequestBody AdCheckDto.Req req) {
        try {
            return ResponseEntity.ok(BaseResponse.success(adCheckService.check(req.getCopy())));
        } catch (RuntimeException e) {
            log.error("Ad copy check failed.", e);
            return ResponseEntity.ok(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        }
    }

    @PostMapping("/check/aijudge")
    public ResponseEntity<BaseResponse<?>> checkWithAiJudge(@RequestBody AdCheckDto.Req req) {
        try {
            return ResponseEntity.ok(BaseResponse.success(adCheckService.checkWithAiJudge(req.getCopy())));
        } catch (RuntimeException e) {
            log.error("Ad copy check via ai-judge failed.", e);
            return ResponseEntity.ok(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        }
    }

    @PostMapping("/check/file")
    public ResponseEntity<BaseResponse<?>> checkFile(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(BaseResponse.success(adCheckService.checkFile(file)));
        } catch (AdCheckService.FileCheckException e) {
            log.error("Ad file check failed after text extraction. fileName={}, size={}",
                    file == null ? null : file.getOriginalFilename(), file == null ? 0 : file.getSize(), e);
            return ResponseEntity.ok(BaseResponse.fail(BaseResponseStatus.FAIL, e.getResponse()));
        } catch (RuntimeException e) {
            log.error("Ad file check failed. fileName={}, size={}", file == null ? null : file.getOriginalFilename(), file == null ? 0 : file.getSize(), e);
            return ResponseEntity.ok(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        }
    }

    @PostMapping("/check/file/aijudge")
    public ResponseEntity<BaseResponse<?>> checkFileWithAiJudge(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "campaignId", required = false) String campaignId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        try {
            return ResponseEntity.ok(BaseResponse.success(adCheckService.checkFileWithAiJudge(file, user, campaignId)));
        } catch (AdCheckService.FileCheckException e) {
            log.error("Ad file check via ai-judge failed with partial response. fileName={}, size={}",
                    file == null ? null : file.getOriginalFilename(), file == null ? 0 : file.getSize(), e);
            return ResponseEntity.ok(BaseResponse.fail(BaseResponseStatus.FAIL, e.getResponse()));
        } catch (RuntimeException e) {
            log.error("Ad file check via ai-judge failed. fileName={}, size={}", file == null ? null : file.getOriginalFilename(), file == null ? 0 : file.getSize(), e);
            return ResponseEntity.ok(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        }
    }
}
