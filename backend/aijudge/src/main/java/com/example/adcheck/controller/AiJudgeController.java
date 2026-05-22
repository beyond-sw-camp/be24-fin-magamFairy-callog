package com.example.adcheck.controller;

import com.example.adcheck.model.AdCheckDto;
import com.example.adcheck.service.AiJudgeFileCheckService;
import com.example.adcheck.service.AiJudgeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/multi/aijudge")
public class AiJudgeController {

    private final AiJudgeService aiJudgeService;
    private final AiJudgeFileCheckService aiJudgeFileCheckService;

    public AiJudgeController(AiJudgeService aiJudgeService, AiJudgeFileCheckService aiJudgeFileCheckService) {
        this.aiJudgeService = aiJudgeService;
        this.aiJudgeFileCheckService = aiJudgeFileCheckService;
    }

    @PostMapping("/check")
    public AdCheckDto.Res check(@RequestBody AdCheckDto.Req req) {
        if (req == null || req.copy() == null || req.copy().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "copy is required.");
        }

        return aiJudgeService.check(req.copy());
    }

    @PostMapping(value = "/check/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdCheckDto.FileCheckRes checkFile(@RequestParam("file") MultipartFile file) {
        return aiJudgeFileCheckService.checkFile(file);
    }

    @ExceptionHandler(AiJudgeFileCheckService.FileCheckException.class)
    public ResponseEntity<AdCheckDto.FileCheckRes> handleFileCheckException(
            AiJudgeFileCheckService.FileCheckException e
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getResponse());
    }
}
