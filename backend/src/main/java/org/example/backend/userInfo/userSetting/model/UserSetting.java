package org.example.backend.userInfo.userSetting.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.common.model.BaseEntity;
import org.example.backend.user.model.User;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Entity
public class UserSetting extends BaseEntity {
    public static final String THEME_LIGHT = "light";
    public static final String DENSITY_COMFORTABLE = "comfortable";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_idx", nullable = false, unique = true)
    private User user;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String theme = THEME_LIGHT;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String density = DENSITY_COMFORTABLE;

    @Builder.Default
    @Column(nullable = false)
    private Boolean reduceMotion = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean highContrast = false;

    public void update(String theme, String density, Boolean reduceMotion, Boolean highContrast) {
        if (theme != null) {
            this.theme = theme;
        }

        if (density != null) {
            this.density = density;
        }

        if (reduceMotion != null) {
            this.reduceMotion = reduceMotion;
        }

        if (highContrast != null) {
            this.highContrast = highContrast;
        }
    }
}
