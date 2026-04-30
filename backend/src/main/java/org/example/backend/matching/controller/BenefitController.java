package org.example.backend.matching.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.model.BaseResponseStatus;
import org.example.backend.matching.model.MatchingDto;
import org.example.backend.matching.service.AssetService;
import org.example.backend.matching.service.BenefitService;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/matching")
@RequiredArgsConstructor
public class BenefitController {
    private final BenefitService benefitService;

    @GetMapping("/benefit/{idx}")
    public ResponseEntity getBenefit(@PathVariable Long idx) {
        try {
            MatchingDto.BenefitRes dto = benefitService.getBenefit(idx);
            return ResponseEntity.ok(BaseResponse.success(dto));
        }
        catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(BaseResponse.fail(BaseResponseStatus.NO_SUCH_ELEMENT, null));
        }
    }

    @GetMapping("/benefit/list")
    public ResponseEntity getBenefitList(
            @RequestParam(required = true, defaultValue = "0") int page,
            @RequestParam(required = true, defaultValue = "10") int size
    ) {
        try {
            MatchingDto.BenefitList dto = benefitService.getBenefitList(page, size);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(BaseResponse.success(BaseResponseStatus.LIST_SUCCESS, dto));
        }
        catch (NoSuchElementException e){
            return ResponseEntity.status(HttpStatus.OK)
                    .body(BaseResponse.fail(BaseResponseStatus.NO_SUCH_ELEMENT));
        }
    }

    @PostMapping("/benefit/add")
    public ResponseEntity addBenefit(@RequestBody MatchingDto.AddBenefit dto,
                                   @AuthenticationPrincipal AuthUserDetails user){
        try {
            benefitService.addBenefit(dto, user);
            return  ResponseEntity.ok(BaseResponse.success(BaseResponseStatus.BENEFIT_ADD_SUCCESS,null));
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL,e.getMessage()));
        }
    }
}
