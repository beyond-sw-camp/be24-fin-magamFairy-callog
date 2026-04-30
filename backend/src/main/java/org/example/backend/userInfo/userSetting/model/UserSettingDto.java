package org.example.backend.userInfo.userSetting.model;

import lombok.Builder;

public class UserSettingDto {
    public record UpdateReq(
            String theme,
            Boolean darkMode,
            String density,
            Boolean reduceMotion,
            Boolean highContrast
    ) {
    }

    @Builder
    public record Res(
            Long idx,
            Long userIdx,
            String theme,
            Boolean darkMode,
            String density,
            Boolean reduceMotion,
            Boolean highContrast
    ) {
        public static Res from(UserSetting entity) {
            return Res.builder()
                    .idx(entity.getIdx())
                    .userIdx(entity.getUser().getIdx())
                    .theme(entity.getTheme())
                    .darkMode("dark".equals(entity.getTheme()))
                    .density(entity.getDensity())
                    .reduceMotion(entity.getReduceMotion())
                    .highContrast(entity.getHighContrast())
                    .build();
        }
    }
}
