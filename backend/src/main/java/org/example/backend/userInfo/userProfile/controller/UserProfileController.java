package org.example.backend.userInfo.userProfile.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.userInfo.userProfile.model.UserProfileDto;
import org.example.backend.userInfo.userProfile.service.UserProfileService;
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
@RequestMapping("/user-profiles")
public class UserProfileController {
    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ResponseEntity<BaseResponse> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(BaseResponse.success(
                userProfileService.getMyProfile(currentUser(authentication))
        ));
    }

    @PatchMapping("/me")
    public ResponseEntity<BaseResponse> updateMyProfile(
            @RequestBody UserProfileDto.UpdateReq dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                userProfileService.updateMyProfile(currentUser(authentication), dto)
        ));
    }

    private String currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }

        return authentication.getName();
    }
}
