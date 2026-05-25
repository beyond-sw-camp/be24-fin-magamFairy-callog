package org.example.notification.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

// Gateway가 전달한 현재 사용자 정보를 컨트롤러에서 쓰기 위한 인증 주체입니다.
public record AuthUser(
        Long userId,
        String loginId,
        String email,
        String name,
        String role,
        Long organizationId,
        String orgType
) {
    // Spring Security 권한 검사에 사용할 권한 목록을 반환합니다.
    public Collection<? extends GrantedAuthority> authorities() {
        String resolvedRole = role == null || role.isBlank() ? "ROLE_USER" : role;
        return List.of(new SimpleGrantedAuthority(resolvedRole));
    }
}
