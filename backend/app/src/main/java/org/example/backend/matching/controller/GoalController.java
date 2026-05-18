package org.example.backend.matching.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.model.BaseResponseStatus;
import org.example.backend.matching.model.MatchingDto;
import org.example.backend.matching.service.GoalService;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/matching")
@RequiredArgsConstructor
public class GoalController {
    private final GoalService goalService;

    @GetMapping("/goal/{idx}")
    public ResponseEntity getGoal(@PathVariable Long idx) {
        try {
            MatchingDto.GoalRes dto = goalService.getGoal(idx);
            return ResponseEntity.ok(BaseResponse.success(dto));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(BaseResponse.fail(BaseResponseStatus.NO_SUCH_ELEMENT));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        }
    }

    @GetMapping("/goal/list")
    public ResponseEntity getGoalList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            MatchingDto.GoalList dto = goalService.getGoalList(page, size);
            return ResponseEntity.ok(BaseResponse.success(BaseResponseStatus.LIST_SUCCESS, dto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        }
    }

    @PostMapping("/goal/add")
    public ResponseEntity addGoal(
            @RequestBody MatchingDto.AddGoal dto,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        try {
            goalService.addGoal(dto, user);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(BaseResponse.success(BaseResponseStatus.SUCCESS));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        }
    }

    @PutMapping("/goal/update/{idx}")
    public ResponseEntity updateGoal(
            @PathVariable Long idx,
            @RequestBody MatchingDto.AddGoal dto
    ) {
        try {
            goalService.updateGoal(idx, dto);
            return ResponseEntity.ok(BaseResponse.success(BaseResponseStatus.SUCCESS));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(BaseResponse.fail(BaseResponseStatus.NO_SUCH_ELEMENT));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        }
    }

    @DeleteMapping("/goal/delete/{idx}")
    public ResponseEntity deleteGoal(@PathVariable Long idx) {
        try {
            goalService.deleteGoal(idx);
            return ResponseEntity.ok(BaseResponse.success(BaseResponseStatus.SUCCESS));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(BaseResponse.fail(BaseResponseStatus.NO_SUCH_ELEMENT));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL, e.getMessage()));
        }
    }
}
