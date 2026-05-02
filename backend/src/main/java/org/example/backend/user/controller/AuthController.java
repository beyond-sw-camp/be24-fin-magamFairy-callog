package org.example.backend.user.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.user.model.TokenDto;
import org.example.backend.user.model.UserDto;
import org.example.backend.user.service.AuthService;
import org.example.backend.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Value("${app.secure-cookie}")
    private boolean secureCookie;

    @PostMapping("/usercreate")
    public ResponseEntity<?> createUser(@RequestBody UserDto.CreateUserReq dto, Authentication authentication) {
        UserDto.CreateUserRes result = userService.createUser(dto, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(result));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> partnerSignup(@RequestBody UserDto.PartnerSignupReq dto) {
        UserDto.PartnerSignupRes result = userService.partnerSignup(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(result));
    }

    @PostMapping("/resetpassword")
    public ResponseEntity<?> resetPassword(@RequestBody UserDto.ResetPasswordReq dto, Authentication authentication) {
        UserDto.ResetPasswordRes result = userService.resetPassword(dto, authentication);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PostMapping("/userdelete")
    public ResponseEntity<?> deleteUser(@RequestBody UserDto.DeleteUserReq dto, Authentication authentication) {
        UserDto.DeleteUserRes result = userService.deleteUser(dto, authentication);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PatchMapping("/password")
    public ResponseEntity<?> changePassword(
            @RequestBody UserDto.ChangePasswordReq dto,
            Authentication authentication
    ) {
        UserDto.ChangePasswordRes result = userService.changeMyPassword(dto, authentication);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = findRefreshCookie(request);
            TokenDto.AuthTokenResponse tokens = authService.reissue(refreshToken);

            response.setHeader("Authorization", "Bearer " + tokens.accessToken());

            return ResponseEntity.ok().body("token reissued");
        } catch (IllegalArgumentException e) {
            response.addCookie(createRefreshCookie(null, 0));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "invalid refresh token",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = findRefreshCookie(request);

        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        response.addCookie(createRefreshCookie(null, 0));

        return ResponseEntity.ok().body("logged out");
    }

    private String findRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if ("refresh".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private Cookie createRefreshCookie(String value, int maxAge) {
        Cookie refreshCookie = new Cookie("refresh", value);
        refreshCookie.setMaxAge(maxAge);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setSecure(secureCookie);
        return refreshCookie;
    }
}
