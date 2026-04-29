package org.example.backend.userInfo.userProfile.model;

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
import lombok.Setter;
import org.example.backend.common.model.BaseEntity;
import org.example.backend.user.model.User;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Entity
public class UserProfile extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_idx", nullable = false, unique = true)
    private User user;

    @Setter
    @Column(length = 120)
    private String email;

    @Setter
    @Column(length = 30)
    private String phone;

    @Setter
    @Column(name = "profile_image_key", length = 512)
    private String profileImageKey;

    @Setter
    @Column(name = "profile_image_url", length = 1000)
    private String profileImageUrl;

    public void update(String email, String phone, String profileImageKey, String profileImageUrl) {
        this.email = email;
        this.phone = phone;
        this.profileImageKey = profileImageKey;
        this.profileImageUrl = profileImageUrl;
    }
}
