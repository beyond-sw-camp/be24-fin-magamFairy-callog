package org.example.backend.userInfo.userProfile.model;

import lombok.Builder;
import org.example.backend.user.model.User;

public class UserProfileDto {
    public record UpdateReq(
            String email,
            String phone
    ) {
    }

    public record ProfileImageUploadUrlReq(
            String fileName,
            String contentType,
            Long fileSize
    ) {
    }

    @Builder
    public record ProfileImageUploadUrlRes(
            String objectKey,
            String uploadUrl,
            Long expiresInSeconds,
            String contentType
    ) {
    }

    public record ProfileImageCommitReq(
            String objectKey
    ) {
    }

    @Builder
    public record Res(
            Long idx,
            Long userIdx,
            String loginId,
            String name,
            String email,
            String phone,
            String profileImageKey,
            String profileImageUrl
    ) {
        public static Res from(UserProfile entity, String profileImageUrl) {
            User user = entity.getUser();

            return Res.builder()
                    .idx(entity.getIdx())
                    .userIdx(user.getIdx())
                    .loginId(user.getId())
                    .name(user.getName())
                    .email(resolveText(entity.getEmail(), user.getEmail()))
                    .phone(entity.getPhone())
                    .profileImageKey(entity.getProfileImageKey())
                    .profileImageUrl(profileImageUrl)
                    .build();
        }
    }

    private static String resolveText(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }

        return fallback;
    }
}
