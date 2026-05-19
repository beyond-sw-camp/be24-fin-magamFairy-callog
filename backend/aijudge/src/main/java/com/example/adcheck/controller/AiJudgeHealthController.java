package com.example.adcheck.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/multi/aijudge")
public class AiJudgeHealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "service", "ai-judge",
                "status", "UP"
        );
    }
}
