package org.example.backend.adcheck.analysis.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.adcheck.analysis.model.AdAnalysisBenchmarkDto.BenchmarkRequest;
import org.example.backend.adcheck.analysis.model.AdAnalysisBenchmarkDto.ReviewScenarioRequest;
import org.example.backend.adcheck.analysis.model.AdAnalysisBenchmarkDto.SyntheticSeedRequest;
import org.example.backend.adcheck.analysis.service.AdAnalysisBenchmarkService;
import org.example.backend.common.model.BaseResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@RequiredArgsConstructor
@RequestMapping("/dev/ad-analysis-benchmark")
public class AdAnalysisBenchmarkController {
    private final AdAnalysisBenchmarkService benchmarkService;

    @PostMapping("/seed")
    public ResponseEntity<BaseResponse<?>> seed(@RequestBody(required = false) SyntheticSeedRequest request) {
        return ResponseEntity.ok(BaseResponse.success(benchmarkService.seed(request)));
    }

    @PostMapping("/run")
    public ResponseEntity<BaseResponse<?>> run(@RequestBody(required = false) BenchmarkRequest request) {
        return ResponseEntity.ok(BaseResponse.success(benchmarkService.run(request)));
    }

    @PostMapping("/review-scenario")
    public ResponseEntity<BaseResponse<?>> reviewScenario(@RequestBody(required = false) ReviewScenarioRequest request) {
        return ResponseEntity.ok(BaseResponse.success(benchmarkService.runReviewScenario(request)));
    }
}
