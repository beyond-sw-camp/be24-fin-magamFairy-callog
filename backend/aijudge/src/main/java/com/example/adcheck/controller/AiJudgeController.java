package com.example.adcheck.controller;

import com.example.adcheck.model.AdCheckDto;
import com.example.adcheck.service.AiJudgeFileCheckService;
import com.example.adcheck.service.AiJudgeService;
import com.example.adcheck.analysis.service.AdCheckAnalysisMongoStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@RestController
@RequestMapping({"/multi/aijudge", "/aijudge"})
public class AiJudgeController {

    private final AiJudgeService aiJudgeService;
    private final AiJudgeFileCheckService aiJudgeFileCheckService;
    private final AdCheckAnalysisMongoStorageService adCheckAnalysisMongoStorageService;

    public AiJudgeController(
            AiJudgeService aiJudgeService,
            AiJudgeFileCheckService aiJudgeFileCheckService,
            AdCheckAnalysisMongoStorageService adCheckAnalysisMongoStorageService
    ) {
        this.aiJudgeService = aiJudgeService;
        this.aiJudgeFileCheckService = aiJudgeFileCheckService;
        this.adCheckAnalysisMongoStorageService = adCheckAnalysisMongoStorageService;
    }

    @PostMapping("/check")
    public AdCheckDto.Res check(@RequestBody AdCheckDto.Req req) {
        if (req == null || req.copy() == null || req.copy().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "copy is required.");
        }

        return aiJudgeService.check(req.copy());
    }

    @PostMapping(value = "/check/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdCheckDto.FileCheckRes checkFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "context", required = false) String context,
            @RequestParam(value = "analysisJobId", required = false) String analysisJobId
    ) {
        return aiJudgeFileCheckService.checkFile(file, context, analysisJobId);
    }

    @GetMapping("/analyses/{analysisJobId}")
    public AdCheckDto.FileCheckRes analysisDetail(@PathVariable String analysisJobId) {
        Optional<AdCheckDto.FileCheckRes> analysis = adCheckAnalysisMongoStorageService.findByAnalysisJobId(analysisJobId);
        return analysis.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "analysis result not found."));
    }

    @ExceptionHandler(AiJudgeFileCheckService.FileCheckException.class)
    public ResponseEntity<AdCheckDto.FileCheckRes> handleFileCheckException(
            AiJudgeFileCheckService.FileCheckException e
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getResponse());
    }
}
