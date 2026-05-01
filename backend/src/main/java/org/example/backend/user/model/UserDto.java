package org.example.backend.user.model;

import lombok.Builder;

public class UserDto {
    public record CreateUserReq(
            String companyName,
            String department,
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

    public record PartnerSignupReq(
            String companyName,
            String department,
            String name,
            String email,
            String phone
    ) {
    }

    @Builder
    public record PartnerSignupRes(
            String id,
            String companyName,
            String name,
            String email,
            String password
    ) {
    }

    public record ResetPasswordReq(String id) {
    }

    @Builder
    public record ResetPasswordRes(String id, String password) {
    }

    public record PromoteToManagerReq(String id) {
    }

    @Builder
    public record PromoteToManagerRes(String id, String name, String role) {
    }

    public record DeleteUserReq(String id) {
    }

    @Builder
    public record DeleteUserRes(
            String id,
            String name,
            String role,
            String companyName,
            String department,
            Boolean deleted
    ) {
        public static DeleteUserRes from(User entity) {
            return DeleteUserRes.builder()
                    .id(entity.getId())
                    .name(entity.getName())
                    .role(entity.getRole())
                    .companyName(entity.getCompanyName())
                    .department(entity.getDepartment())
                    .deleted(!Boolean.TRUE.equals(entity.getEnable()))
                    .build();
        }
    }
}
