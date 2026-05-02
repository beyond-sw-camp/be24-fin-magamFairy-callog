package org.example.backend.matching.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.model.BaseResponseStatus;
import org.example.backend.matching.model.MatchingDto;
import org.example.backend.matching.service.AssetService;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/matching")
@RequiredArgsConstructor
public class AssetController {
    private final AssetService assetService;

    @GetMapping("/asset/{idx}")
    public ResponseEntity getAsset(@PathVariable Long idx) {
        try {
            MatchingDto.AssetRes dto = assetService.getAsset(idx);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(BaseResponse.success(dto));
        }
        catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(BaseResponse.fail(BaseResponseStatus.NO_SUCH_ELEMENT));
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
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
            return ResponseEntity.status(HttpStatus.OK)
                    .body(BaseResponse.success(BaseResponseStatus.LIST_SUCCESS, dto));
        }
        catch (NoSuchElementException e){
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(BaseResponse.fail(BaseResponseStatus.NO_SUCH_ELEMENT));
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL,e.getMessage()));
        }
    }

    @PostMapping("/asset/add")
    public ResponseEntity addAsset(@RequestBody MatchingDto.AddAsset dto,
                                   @AuthenticationPrincipal AuthUserDetails user){
        try {
            assetService.addAsset(dto, user);
            return  ResponseEntity.status(HttpStatus.CREATED)
                    .body(BaseResponse.success(BaseResponseStatus.ASSET_ADD_SUCCESS));
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT)
                    .body(BaseResponse.fail(BaseResponseStatus.FAIL,e.getMessage()));
        }
    }
}
