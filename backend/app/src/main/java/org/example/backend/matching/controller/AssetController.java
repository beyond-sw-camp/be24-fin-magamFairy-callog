package org.example.backend.matching.controller;

import jakarta.persistence.EntityNotFoundException;
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
        MatchingDto.AssetRes dto = assetService.getAsset(idx);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(dto));
    }

    @GetMapping("/asset/list")
    public ResponseEntity getAssetList(
            @RequestParam(required = true, defaultValue = "0") int page,
            @RequestParam(required = true, defaultValue = "10") int size
    ) {
        MatchingDto.AssetList dto = assetService.getAssetList(page, size);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(BaseResponseStatus.LIST_SUCCESS, dto));
    }

    @PostMapping("/asset/add")
    public ResponseEntity addAsset(@RequestBody MatchingDto.AddAsset dto,
                                   @AuthenticationPrincipal AuthUserDetails user){
        assetService.addAsset(dto, user);
        return  ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(BaseResponseStatus.ASSET_ADD_SUCCESS));
    }

    @PutMapping("/asset/{idx}")
    public ResponseEntity updateAsset(@PathVariable Long idx, @RequestBody MatchingDto.AddAsset dto) {
        assetService.updateAsset(idx, dto);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(BaseResponseStatus.SUCCESS));
    }

    @DeleteMapping("/asset/{idx}")
    public ResponseEntity deleteAsset(@PathVariable Long idx) {
        assetService.deleteAsset(idx);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(BaseResponseStatus.SUCCESS));
    }
}
