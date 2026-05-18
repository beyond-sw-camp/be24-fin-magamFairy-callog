package org.example.backend.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.backend.organization.model.Organization;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(indexes = {
        @Index(name = "idx_user_id", columnList = "id"),
        @Index(name = "idx_user_email", columnList = "email")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Setter
    @Column(nullable = false, unique = true)
    private String id;

    @Setter
    @Column(unique = true)
    private String email;

    @Setter
    @Column(nullable = false)
    private String name;

    @Setter
    @Column
    private String companyName;

    @Setter
    @Column
    private String department;

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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Setter
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserAccountStatus accountStatus = UserAccountStatus.ACTIVE;
}
