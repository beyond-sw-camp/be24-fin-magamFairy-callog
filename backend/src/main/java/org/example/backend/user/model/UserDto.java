package org.example.backend.user.model;

import lombok.Builder;

public class UserDto {
    public record CreateUserReq(
            String teamCode,
            String name,
            String email,
            String role
    ) {
    }

    @Builder
    public record CreateUserRes(
            Long idx,
            String id,
            String loginId,
            String email,
            String name,
            String role,
            String password,
            boolean passwordResetRequired
    ) {
        public static CreateUserRes from(User entity, String temporaryPassword) {
            return CreateUserRes.builder()
                    .idx(entity.getIdx())
                    .id(entity.getLoginId())
                    .loginId(entity.getLoginId())
                    .email(entity.getEmail())
                    .name(entity.getName())
                    .role(entity.getRole())
                    .password(temporaryPassword)
                    .passwordResetRequired(Boolean.TRUE.equals(entity.getPasswordResetRequired()))
                    .build();
        }
    }

    public record LoginReq(String email, String loginId, String password) {
        public String username() {
            return loginId != null && !loginId.isBlank() ? loginId : email;
        }
    }
}
