package com.example.adcheck.controller;

import com.example.adcheck.model.AdCheckDto;
import com.example.adcheck.service.AiJudgeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/multi/aijudge")
public class AiJudgeController {

    private final AiJudgeService aiJudgeService;

    public AiJudgeController(AiJudgeService aiJudgeService) {
        this.aiJudgeService = aiJudgeService;
    }

    @PostMapping("/check")
    public AdCheckDto.Res check(@RequestBody AdCheckDto.Req req) {
        if (req == null || req.copy() == null || req.copy().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "copy is required.");
        }

        return aiJudgeService.check(req.copy());
    }
}
