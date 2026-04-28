package org.example.backend.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(indexes = {
        @Index(name = "idx_user_login_id", columnList = "loginId"),
        @Index(name = "idx_user_email", columnList = "email")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Setter
    @Column(nullable = false, unique = true)
    private String loginId;

    @Setter
    @Column(unique = true)
    private String email;

    @Setter
    @Column(nullable = false)
    private String name;

    @Setter
    @Column(nullable = false)
    private String password;

    @Setter
    @Builder.Default
    @Column(nullable = false)
    private Boolean enable = true;

    @Setter
    @Builder.Default
    @Column(nullable = false)
    private String role = "ROLE_USER";

    @Setter
    @Builder.Default
    @Column(nullable = false)
    private Boolean passwordResetRequired = true;

    @Setter
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserAccountStatus accountStatus = UserAccountStatus.ACTIVE;
}
