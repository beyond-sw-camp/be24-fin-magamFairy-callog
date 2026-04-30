package org.example.backend.common.security;

import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class RoleGuard {

    public static final String ROLE_MANAGER = "ROLE_MANAGER";

    private RoleGuard() {}

    public static AuthUserDetails requireAuthenticated(AuthUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 유저 정보가 없습니다.");
        }
        return user;
    }

    public static AuthUserDetails requireManager(AuthUserDetails user) {
        AuthUserDetails authenticated = requireAuthenticated(user);
        if (!ROLE_MANAGER.equals(authenticated.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ROLE_MANAGER 권한이 필요합니다.");
        }
        return authenticated;
    }
}
