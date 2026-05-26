package org.example.backend.user.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.user.model.TokenDto;
import org.example.backend.user.model.UserDto;
import org.example.backend.user.service.AuthService;
import org.example.backend.user.service.JwtBlacklistService;
import org.example.backend.user.service.UserService;
import org.example.backend.user.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtBlacklistService jwtBlacklistService;
    private final JwtUtil jwtUtil;

    @Value("${app.secure-cookie}")
    private boolean secureCookie;

    @PostMapping("/usercreate")
    public ResponseEntity<?> createUser(@RequestBody UserDto.CreateUserReq dto, Authentication authentication) {
        UserDto.CreateUserRes result = userService.createUser(dto, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(result));
    }

    @PostMapping("/manage")
    public ResponseEntity<?> manageUserRole(@RequestBody UserDto.ManageRoleReq dto, Authentication authentication) {
        UserDto.ManageRoleRes result = userService.manageUserRole(dto, authentication);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @GetMapping("/manage/users")
    public ResponseEntity<?> listManageableUsers(Authentication authentication) {
        return ResponseEntity.ok(BaseResponse.success(userService.listManageableUsers(authentication)));
    }

    @GetMapping("/users/colleagues")
    public ResponseEntity<?> listColleagues(Authentication authentication) {
        return ResponseEntity.ok(BaseResponse.success(userService.listColleagues(authentication)));
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

    @PatchMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody UserDto.ChangePasswordReq dto, Authentication authentication) {
        UserDto.ChangePasswordRes result = userService.changePassword(dto, authentication);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PostMapping("/userdelete")
    public ResponseEntity<?> deleteUser(@RequestBody UserDto.DeleteUserReq dto, Authentication authentication) {
        UserDto.DeleteUserRes result = userService.deleteUser(dto, authentication);
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

        // 로그아웃을 할 시 현재 유저의 accessToken을 블랙리스트에 올림 * 보안
        String auth = request.getHeader("Authorization");
        if(auth != null && auth.startsWith("Bearer ")) {
            String access = auth.substring(7).trim();

            try {
                long remainMs = jwtUtil.getExpiration(access).getTime() - System.currentTimeMillis();
                jwtBlacklistService.blacklist(access, Duration.ofMillis(Math.max(0, remainMs)));
            } catch (Exception ignored) {
                // 잘못된 토큰이면 무시 (어차피 JwtFilter에서 거부됨)
            }
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
