package org.example.evaluation.controller;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.example.evaluation.common.model.BaseResponse;
import org.example.evaluation.common.model.BaseResponseStatus;
import org.example.evaluation.model.EvaluationDto;
import org.example.evaluation.model.N8nEvaluationPayloadDto;
import org.example.evaluation.service.EvaluationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/evaluation")
@RequiredArgsConstructor
public class EvaluationController {
    private final EvaluationService evaluationService;

    @GetMapping("/result")
    public ResponseEntity<BaseResponse> getEvaluation(@RequestParam String campaignIdx) {
            return ResponseEntity.ok(
                    BaseResponse.processing(
                            BaseResponseStatus.SUCCESS,
                            evaluationService.result(campaignIdx)
                    )
            );
    }

    @PostMapping("/collect")
    public ResponseEntity<BaseResponse> collect(@RequestBody N8nEvaluationPayloadDto dto) {

        if (dto == null) {
            return ResponseEntity.badRequest().body(BaseResponse.fail(BaseResponseStatus.EMPTY_PAYLOAD));
        }
        evaluationService.collect(dto);
        return ResponseEntity.accepted()
                .body(BaseResponse.processing(BaseResponseStatus.SUCCESSFULY_EVALUATED));
    }

    @PostMapping("/start")
    public ResponseEntity<BaseResponse> start(@RequestBody EvaluationDto.StartEvaluationReq dto) {
        evaluationService.requestStartEvaluation(dto);

        return ResponseEntity.accepted()
                .body(BaseResponse.processing(BaseResponseStatus.EVLUATION_STARTED));
    }
}
