package org.example.backend.matching.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.model.BaseResponseStatus;
import org.example.backend.matching.model.MatchingDto;
import org.example.backend.matching.model.evaluation.EvaluationDto;
import org.example.backend.matching.service.EvaluationService;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/matching")
@RequiredArgsConstructor
public class EvaluationController {
    private final EvaluationService evaluationService;

    @GetMapping("/evaluation/result")
    public ResponseEntity<BaseResponse> getEvaluation(@RequestParam String campaignIdx) {
        List<EvaluationDto.MongoEvaluationRes> dto = evaluationService.result(campaignIdx);
        return  ResponseEntity.ok(BaseResponse.processing(BaseResponseStatus.SUCCESS, dto));
    }

    @PostMapping("/evaluation/collect")
    public ResponseEntity collect(@RequestBody EvaluationDto.CollectDto dto){
        evaluationService.collect(dto);
        return  ResponseEntity.ok(BaseResponse.processing(BaseResponseStatus.SUCCESSFULY_EVALUATED, dto));
    }

    @PostMapping("/evaluation/start")
    public ResponseEntity startEvaluation(@RequestBody EvaluationDto.StartEvaluationReq dto){
    evaluationService.startEvaluation(dto);
    return  ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(BaseResponse.processing(BaseResponseStatus.EVLUATION_STARTED));
    }
}
