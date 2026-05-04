package org.example.backend.userInfo.userSetting.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.userInfo.userSetting.model.UserSettingDto;
import org.example.backend.userInfo.userSetting.service.UserSettingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/settings")
public class UserSettingController {
    private final UserSettingService userSettingService;

    @GetMapping
    public ResponseEntity<BaseResponse> getMySetting(Authentication authentication) {
        return ResponseEntity.ok(BaseResponse.success(
                userSettingService.getMySetting(currentUser(authentication))
        ));
    }

    @PatchMapping
    public ResponseEntity<BaseResponse> updateMySetting(
            @RequestBody UserSettingDto.UpdateReq dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                userSettingService.updateMySetting(currentUser(authentication), dto)
        ));
    }

    private String currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }

        return authentication.getName();
    }
}
