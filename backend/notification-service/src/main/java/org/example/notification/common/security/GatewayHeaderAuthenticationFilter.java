package org.example.notification.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Gateway가 검증 후 전달한 사용자 헤더로 AuthUser를 구성하는 필터입니다.
@Component
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_IDX_HEADER = "X-User-Idx";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_NAME_HEADER = "X-User-Name";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String ORGANIZATION_ID_HEADER = "X-Organization-Id";
    private static final String USER_ORGANIZATION_ID_HEADER = "X-User-Organization-Id";
    private static final String USER_ORG_TYPE_HEADER = "X-User-OrgType";

    // 인증이 필요 없는 내부 상태 확인 요청은 사용자 헤더를 검사하지 않습니다.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/actuator")
                || path.startsWith("/error");
    }

    // Gateway에서 전달된 헤더를 읽어 Spring Security 인증 객체를 등록합니다.
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        SecurityContextHolder.clearContext();

        String userIdx = header(request, USER_IDX_HEADER);
        if (userIdx == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AuthUser authUser = new AuthUser(
                    parseLong(userIdx),
                    header(request, USER_ID_HEADER),
                    header(request, USER_EMAIL_HEADER),
                    header(request, USER_NAME_HEADER),
                    resolveRole(header(request, USER_ROLE_HEADER)),
                    parseOptionalLong(firstNonBlank(
                            header(request, ORGANIZATION_ID_HEADER),
                            header(request, USER_ORGANIZATION_ID_HEADER)
                    )),
                    header(request, USER_ORG_TYPE_HEADER)
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(authUser, null, authUser.authorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (NumberFormatException exception) {
            SecurityContextHolder.clearContext();
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid gateway user context");
        }
    }

    private String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String first, String second) {
        return first != null ? first : second;
    }

    private Long parseLong(String value) {
        return Long.parseLong(value);
    }

    private Long parseOptionalLong(String value) {
        return value == null ? null : parseLong(value);
    }

    private String resolveRole(String role) {
        return role == null || role.isBlank() ? "ROLE_USER" : role;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().print("{\"error\":\"" + message + "\"}");
    }
}
