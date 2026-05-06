package org.example.backend.matching.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.model.BaseResponseStatus;
import org.example.backend.matching.model.evaluation.EvaluationDto;
import org.example.backend.matching.service.EvaluationService;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/matching")
@RequiredArgsConstructor
public class EvaluationController {
    private final EvaluationService evaluationService;

    @PostMapping("/evaluation/collect")
    public ResponseEntity collect(@RequestBody EvaluationDto.CollectDto dto,
                                        @RequestParam String category){
        try {
            evaluationService.collect(dto, category);
            return  ResponseEntity.ok(BaseResponse.processing(BaseResponseStatus.SUCCESSFULY_EVALUATED, dto));
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL,e.getMessage()));
        }
    }

    @PostMapping("/evaluation/start")
    public ResponseEntity startEvaluation(@RequestBody EvaluationDto.StartEvaluationReq dto){
        try {
            evaluationService.startEvaluation(dto);
            return  ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(BaseResponse.processing(BaseResponseStatus.EVLUATION_STARTED));
        }
        catch (EntityNotFoundException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail(BaseResponseStatus.NO_SUCH_ELEMENT,e.getMessage()));
        }
        catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.fail(BaseResponseStatus.SERVER_NOT_RESPONDING,e.getMessage()));
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL,e.getMessage()));
        }
    }

//    @GetMapping("/evaluation/list/{campaign_idx}")
//    public ResponseEntity getEvaluationList(@AuthenticationPrincipal AuthUserDetails user, @PathVariable Long campaign_idx) {
//        ResponseEntity result;
//        try {
//            EvaluationDto.EvaluationRes dto = evaluationService.getEvaluationRes(user.getIdx(), campaign_idx);
//            result = ResponseEntity.ok(BaseResponse.success(dto));
//        }
//        catch (AccessDeniedException e) {
//            result = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(BaseResponse.fail(BaseResponseStatus.ACCESS_DENIED, null));
//        }
//        catch (Exception e){
//            result = ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
//                    .body(BaseResponse.fail(BaseResponseStatus.FAIL,e.getMessage()));
//        }
//        return result;
//    }
}
