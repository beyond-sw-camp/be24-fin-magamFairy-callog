package org.example.backend.adcheck.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.adcheck.model.AdCheckJobDto;
import org.example.backend.adcheck.model.AdCheckDto;
import org.example.backend.adcheck.service.AdCheckJobService;
import org.example.backend.adcheck.service.AdCheckService;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.model.BaseResponseStatus;
import org.example.backend.common.security.RoleGuard;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final AdCheckJobService adCheckJobService;

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
            log.error("Ad file check via ai-judge failed with partial response. fileName={}, size={}",
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

    @PostMapping("/check/jobs")
    public ResponseEntity<BaseResponse<AdCheckJobDto.JobRes>> createCheckJob(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "campaignId", required = false) String campaignId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(adCheckJobService.createJob(file, user, campaignId)));
    }

    @PostMapping("/check/jobs/direct")
    public ResponseEntity<BaseResponse<AdCheckJobDto.DirectJobRes>> createDirectCheckJob(
            @RequestBody AdCheckJobDto.DirectJobCreateReq request,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(adCheckJobService.createDirectJob(request, user)));
    }

    @PostMapping("/check/jobs/internal/progress")
    public ResponseEntity<BaseResponse<AdCheckJobDto.JobRes>> updateCheckJobProgress(
            @RequestBody AdCheckJobDto.ProgressReq request
    ) {
        return ResponseEntity.ok(BaseResponse.success(adCheckJobService.updateProgress(request)));
    }

    @GetMapping("/check/jobs")
    public ResponseEntity<BaseResponse<?>> listCheckJobs(
            @RequestParam(value = "campaignId", required = false) String campaignId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(adCheckJobService.listJobs(user, campaignId)));
    }

    @GetMapping("/check/jobs/active")
    public ResponseEntity<BaseResponse<?>> getActiveCheckJobs(@AuthenticationPrincipal AuthUserDetails user) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(adCheckJobService.getActiveJobs(user)));
    }

    @GetMapping("/check/jobs/{jobId}")
    public ResponseEntity<BaseResponse<AdCheckJobDto.JobRes>> getCheckJob(
            @PathVariable String jobId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(adCheckJobService.getJob(jobId, user)));
    }

    @GetMapping("/check/jobs/{jobId}/detail")
    public ResponseEntity<BaseResponse<AdCheckJobDto.JobDetailRes>> getCheckJobDetail(
            @PathVariable String jobId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(adCheckJobService.getJobDetail(jobId, user)));
    }

    @PostMapping("/check/jobs/{jobId}/cancel")
    public ResponseEntity<BaseResponse<AdCheckJobDto.JobRes>> cancelCheckJob(
            @PathVariable String jobId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(adCheckJobService.cancelJob(jobId, user)));
    }

    @DeleteMapping("/check/jobs/{jobId}")
    public ResponseEntity<BaseResponse<Void>> deleteCheckJob(
            @PathVariable String jobId,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        adCheckJobService.deleteJob(jobId, user);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
