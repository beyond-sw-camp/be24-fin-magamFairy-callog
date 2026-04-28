package org.example.backend.user.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.backend.user.model.TokenDto;
import org.example.backend.user.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.secure-cookie}")
    private boolean secureCookie;

    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = findRefreshCookie(request);
        TokenDto.AuthTokenResponse tokens = authService.reissue(refreshToken);

        response.setHeader("Authorization", "Bearer " + tokens.accessToken());
        response.addCookie(createRefreshCookie(tokens.refreshToken(), 14 * 24 * 60 * 60));

        return ResponseEntity.ok().body("token reissued");
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
