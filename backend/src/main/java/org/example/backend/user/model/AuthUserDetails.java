package org.example.backend.user.model;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Builder
public class AuthUserDetails implements UserDetails {
    private Long idx;
    private String id;
    private String email;
    private String password;
    private Boolean enable;
    private String role;
    private String name;
    private UserAccountStatus accountStatus;

    public static AuthUserDetails from(User entity) {
        return AuthUserDetails.builder()
                .idx(entity.getIdx())
                .id(entity.getId())
                .email(entity.getEmail())
                .name(entity.getName())
                .password(entity.getPassword())
                .enable(entity.getEnable())
                .role(entity.getRole())
                .accountStatus(entity.getAccountStatus())
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String resolvedRole = role == null || role.isBlank() ? "ROLE_USER" : role;
        return List.of(new SimpleGrantedAuthority(resolvedRole));
    }

    @Override
    public String getUsername() {
        return id;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountStatus == null || accountStatus == UserAccountStatus.ACTIVE;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enable);
    }
}
