package org.example.springgw.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class JwtFilter extends AbstractGatewayFilterFactory<JwtFilter.Config> {

    private final JwtUtil jwtUtil;

    public static class Config {
        // 필터 확장 설정이 필요하면 사용
    }

    public JwtFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // 1. 기존 shouldNotFilter에 있던 프리패스 주소 정렬
            if (path.startsWith("/auth/login") ||
                    path.startsWith("/login") ||
                    path.startsWith("/auth/reissue") ||
                    path.startsWith("/auth/logout")) {
                return chain.filter(exchange);
            }

            // 2. 헤더에서 Authorization 추출
            String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // 3. 기존 코드의 SSE(EventSource) 요청 대응: 쿼리 파라미터에서 토큰 파싱
            if (authorization == null && isSseRequest(path)) {
                String tokenParam = request.getQueryParams().getFirst("token");
                if (tokenParam != null) {
                    authorization = "Bearer " + tokenParam;
                }
            }

            // 4. 인증 헤더가 아예 없거나 잘못된 포맷인 경우 다음 체인으로 (시큐리티가 통제하도록 방관 혹은 차단)
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return chain.filter(exchange);
            }

            String token = authorization.substring(7).trim();
            if (token.isBlank()) {
                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "invalid token");
            }

            try {
                // 5. 토큰 검증 및 Payload 복호화
                Claims claims = jwtUtil.parseClaims(token);

                // 6. 기존 코드 로직: 'access' 토큰 카테고리 검증
                String category = claims.get("category", String.class);
                if (!"access".equals(category)) {
                    return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "invalid token category");
                }

                // 7. 💡 대망의 하이라이트: 파싱된 클레임 데이터를 하위 모듈이 가져다 쓰도록 헤더에 바인딩
                String idx = claims.get("idx") != null ? claims.get("idx").toString() : "";
                String id = claims.get("id", String.class);
                String email = claims.get("email", String.class);
                String role = claims.get("role", String.class);
                String name = claims.get("name", String.class);
                String orgType = claims.get("orgType", String.class);

                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Idx", idx)
                        .header("X-User-Id", id != null ? id : email)
                        .header("X-User-Email", email)
                        .header("X-User-Role", role == null || role.isBlank() ? "ROLE_USER" : role)
                        .header("X-User-Name", name)
                        .header("X-User-OrgType", orgType)
                        .build();

                // 8. 변경된 헤더 요청을 가득 싣고 뒤쪽 서비스로 토스!
                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (ExpiredJwtException e) {
                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "access token expired");
            } catch (Exception e) {
                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "invalid token");
            }
        };
    }

    private boolean isSseRequest(String path) {
        return path.contains("/sse/connect") || path.contains("/notifications/subscribe");
    }

    // 💡 WebFlux식 비동기 JSON 에러 출력 변환 핸들러
    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");

        String jsonError = String.format("{\"error\": \"%s\"}", message);
        byte[] bytes = jsonError.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}