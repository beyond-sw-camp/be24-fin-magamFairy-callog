package org.example.backend.config.filter;


import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.redis.CachedUserAuth;
import org.example.backend.common.redis.UserAuthCache;
import org.example.backend.user.model.AuthUserDetails;
import org.example.backend.user.model.UserAccountStatus;
import org.example.backend.user.service.JwtBlacklistService;
import org.example.backend.user.utils.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserAuthCache userAuthCache;
    private final JwtBlacklistService jwtBlacklistService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth/login")
                || path.startsWith("/login")
                || path.startsWith("/auth/reissue")
                || path.startsWith("/auth/logout");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 헤더에서 Authorization 키를 찾음
        String authorization = request.getHeader("Authorization");

        // EventSource cannot send Authorization headers, so SSE accepts an access token query parameter.
        if (authorization == null && isSseRequest(request)) {
            String tokenParam = request.getParameter("token");
            if (tokenParam != null) {
                authorization = "Bearer " + tokenParam;
            }
        }

        // 2. Authorization 헤더가 없거나 Bearer 접두사가 아니면 검증 종료 (다음 필터로)
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. "Bearer " 부분을 제거하고 순수 토큰 문자열만 추출
        String token = authorization.substring(7).trim();
        if (token.isBlank()) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid token");
            return;
        }

        // 4. 토큰 소멸 시간 검증
        try {
            jwtUtil.isExpired(token);
        } catch (ExpiredJwtException e) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "access token expired");
            return;
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter writer = response.getWriter();
            writer.print("{\"error\": \"invalid token\"}");
            return;
        }

        // 5. 토큰 카테고리 검증 (access 토큰이 맞는지)
        String category = jwtUtil.getCategory(token);
        if (!"access".equals(category)) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid token category");
            return;
        }
        if (jwtBlacklistService.isBlacklisted(token)) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "blacklisted token");
            return;
        }

        Long idx = jwtUtil.getUserIdx(token);
        // User 엔티티(password 포함) 대신 CachedUserAuth DTO를 캐시에서 조회
        CachedUserAuth userEntity;
        try {
            userEntity = userAuthCache.loadUser(idx);
        } catch (ResponseStatusException e) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "user access blocked");
            return;
        }
        if (userEntity == null
                || !Boolean.TRUE.equals(userEntity.enable())
                || userEntity.accountStatus() != UserAccountStatus.ACTIVE) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "user access blocked");
            return;
        }

        String email = jwtUtil.getEmail(token);
        String role = jwtUtil.getRole(token);
        String resolvedRole = role == null || role.isBlank() ? "ROLE_USER" : role;
        String id = jwtUtil.getId(token);
        if (id == null || id.isBlank()) {
            id = email;
        }

        AuthUserDetails user = AuthUserDetails.builder()
                .idx(idx)
                .id(id)
                .email(email)
                .role(resolvedRole)
                .name(userEntity.name())
                .enable(userEntity.enable())
                .accountStatus(userEntity.accountStatus())
                .build();

        String orgType = jwtUtil.getOrgType(token);
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(resolvedRole));
        if (orgType != null) {
            authorities.add(new SimpleGrantedAuthority("ORG_" + orgType));
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    private boolean isSseRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.contains("/sse/connect") || uri.contains("/notifications/subscribe");
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.print("{\"error\": \"" + message + "\"}");
    }
}
