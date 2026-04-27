package org.example.backend.config.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.backend.user.model.AuthUserDetails;
import org.example.backend.user.model.TokenDto;
import org.example.backend.user.model.UserDto;
import org.example.backend.user.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final boolean secureCookie;

    public LoginFilter(
            AuthenticationManager authenticationManager,
            AuthService authService,
            @Value("${app.secure-cookie}") boolean secureCookie) {

        super(authenticationManager);
        this.authenticationManager = authenticationManager;
        this.authService = authService;
        this.secureCookie = secureCookie;
        setFilterProcessesUrl("/auth/login");
    }

    @Override
    protected void successfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain,
            Authentication authResult) throws IOException, ServletException {

        AuthUserDetails user = (AuthUserDetails) authResult.getPrincipal();

        TokenDto.AuthTokenResponse tokens = authService.issueTokens(
                user.getIdx(),
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );

        response.setHeader("Authorization", "Bearer " + tokens.accessToken());

        Cookie refreshCookie = new Cookie("refresh", tokens.refreshToken());
        refreshCookie.setMaxAge(14 * 24 * 60 * 60);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setSecure(secureCookie);
        response.addCookie(refreshCookie);

        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accessToken", tokens.accessToken());
        body.put("id", user.getId());
        body.put("loginId", user.getId());
        body.put("email", user.getEmail());
        body.put("name", user.getName());
        body.put("role", user.getRole());
        new ObjectMapper().writeValue(response.getWriter(), body);
    }

    @Override
    protected void unsuccessfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException failed) throws IOException, ServletException {

        response.getWriter().write("로그인 실패");
    }

    @Override
    public Authentication attemptAuthentication(
            HttpServletRequest request,
            HttpServletResponse response) throws AuthenticationException {

        try {
            UserDto.LoginReq dto = new ObjectMapper().readValue(
                    request.getInputStream(),
                    UserDto.LoginReq.class);

            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(
                            dto.username(),
                            dto.password(),
                            null);

            return authenticationManager.authenticate(token);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
