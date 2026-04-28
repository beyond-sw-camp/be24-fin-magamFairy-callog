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
            String email,
            String name,
            String role,
            String password
    ) {
        public static CreateUserRes from(User entity, String temporaryPassword) {
            return CreateUserRes.builder()
                    .idx(entity.getIdx())
                    .id(entity.getId())
                    .email(entity.getEmail())
                    .name(entity.getName())
                    .role(entity.getRole())
                    .password(temporaryPassword)
                    .build();
        }
    }

    public record LoginReq(String email, String id, String password) {
        public String username() {
            return id != null && !id.isBlank() ? id : email;
        }
    }
}
