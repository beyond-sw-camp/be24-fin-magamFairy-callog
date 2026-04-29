package org.example.backend.matching.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.model.BaseResponseStatus;
import org.example.backend.matching.model.MatchingDto;
import org.example.backend.matching.service.AssetService;
import org.hibernate.JDBCException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/matching")
@RequiredArgsConstructor
public class MatchingController {
    private final AssetService assetService;

    @GetMapping("/asset/{idx}")
    public ResponseEntity getAsset(@PathVariable Long idx) {
        try {
            MatchingDto.AssetRes dto = assetService.getAsset(idx);
            return ResponseEntity.ok(BaseResponse.success(dto));
        }
        catch (JDBCException e) {
            return ResponseEntity.status(HttpStatus.OK)
//                    .body(BaseResponse.success(e.getMessage()));
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL,e.getMessage()));
        }
    }

    @GetMapping("/asset/list")
    public ResponseEntity getAssetList(
            @RequestParam(required = true, defaultValue = "0") int page,
            @RequestParam(required = true, defaultValue = "10") int size
    ) {
        try {
            MatchingDto.AssetList dto = assetService.getAssetList(page, size);
            return ResponseEntity.ok(BaseResponse.success(dto));
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.OK)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL,e.getMessage()));
        }
    }
//
//    @PostMapping("/assets/add")
//    public ResponseEntity addAsset(@RequestBody ){
//
//    }
}
