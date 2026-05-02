package org.example.backend.matching.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.model.BaseResponseStatus;
import org.example.backend.matching.model.EvaluationDto;
import org.example.backend.matching.model.MatchingDto;
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

    @PostMapping("/evaluation/new")
    public ResponseEntity newEvaluation(@RequestBody EvaluationDto.NewEvaluation dto,
                                   @AuthenticationPrincipal AuthUserDetails user){
        try {
            evaluationService.newEvaluation(dto, user);
            return  ResponseEntity.ok(BaseResponse.success(BaseResponseStatus.SUCCESSFULY_EVALUATED));
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL,e.getMessage()));
        }
    }

    @GetMapping("/evaluation/list/{campaign_idx}")
    public ResponseEntity getEvaluationList(@AuthenticationPrincipal AuthUserDetails user, @PathVariable Long campaign_idx) {
        ResponseEntity result;
        try {
            EvaluationDto.EvaluationRes dto = evaluationService.getEvaluationRes(user.getIdx(), campaign_idx);
            result = ResponseEntity.ok(BaseResponse.success(dto));
        }
        catch (AccessDeniedException e) {
            result = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.fail(BaseResponseStatus.ACCESS_DENIED, null));
        }
        catch (Exception e){
            result = ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL,e.getMessage()));
        }
        return result;
    }
}
