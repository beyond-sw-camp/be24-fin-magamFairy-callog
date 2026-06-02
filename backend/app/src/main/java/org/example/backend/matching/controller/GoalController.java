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
        MatchingDto.GoalRes dto = goalService.getGoal(idx);
        return ResponseEntity.ok(BaseResponse.success(dto));
    }

    @GetMapping("/goal/list")
    public ResponseEntity getGoalList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        MatchingDto.GoalList dto = goalService.getGoalList(page, size);
        return ResponseEntity.ok(BaseResponse.success(BaseResponseStatus.LIST_SUCCESS, dto));
    }

    @PostMapping("/goal/add")
    public ResponseEntity addGoal(
            @RequestBody MatchingDto.AddGoal dto,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        goalService.addGoal(dto, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(BaseResponseStatus.SUCCESS));
    }

    @PutMapping("/goal/update/{idx}")
    public ResponseEntity updateGoal(
            @PathVariable Long idx,
            @RequestBody MatchingDto.AddGoal dto
    ) {
        goalService.updateGoal(idx, dto);
        return ResponseEntity.ok(BaseResponse.success(BaseResponseStatus.SUCCESS));
    }

    @DeleteMapping("/goal/delete/{idx}")
    public ResponseEntity deleteGoal(@PathVariable Long idx) {
        goalService.deleteGoal(idx);
        return ResponseEntity.ok(BaseResponse.success(BaseResponseStatus.SUCCESS));
    }
}
