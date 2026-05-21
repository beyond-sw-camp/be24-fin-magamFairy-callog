package org.example.evaluation.controller;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.example.evaluation.common.model.BaseResponse;
import org.example.evaluation.common.model.BaseResponseStatus;
import org.example.evaluation.model.EvaluationDocument;
import org.example.evaluation.model.EvaluationDto;
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
        try {
            return ResponseEntity.ok(
                    BaseResponse.processing(
                            BaseResponseStatus.SUCCESS,
                            evaluationService.getByCampaignIdx(campaignIdx)
                    )
            );
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(BaseResponse.fail(BaseResponseStatus.NO_SUCH_ELEMENT, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        }
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<BaseResponse> getEvaluationBySessionId(@PathVariable String sessionId) {
        try {
            EvaluationDocument document = evaluationService.getBySessionId(sessionId);
            return ResponseEntity.ok(
                    BaseResponse.processing(
                            BaseResponseStatus.SUCCESS,
                            EvaluationDto.MongoEvaluationRes.of(document)
                    )
            );
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(BaseResponse.fail(BaseResponseStatus.NO_SUCH_ELEMENT, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        }
    }

    @PostMapping("/collect")
    public ResponseEntity<BaseResponse> collect(@RequestBody EvaluationDto.SaveEvaluationReq dto) {
        try {
            EvaluationDocument saved = evaluationService.save(dto);
            return ResponseEntity.ok(
                    BaseResponse.processing(
                            BaseResponseStatus.SUCCESSFULY_EVALUATED,
                            EvaluationDto.MongoEvaluationRes.of(saved)
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        }
    }

    @PostMapping("/start")
    public ResponseEntity<BaseResponse> start(@RequestBody EvaluationDto.StartEvaluationReq dto) {
        evaluationService.startEvaluation(dto);

        return ResponseEntity.accepted()
                .body(BaseResponse.processing(BaseResponseStatus.EVLUATION_STARTED));
    }
}
