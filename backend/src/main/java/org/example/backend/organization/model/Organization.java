package org.example.backend.organization.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "organizations")
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Setter
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Setter
    @Column(nullable = false, length = 120)
    private String name;

    @Setter
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OrganizationType type = OrganizationType.AFFILIATE;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_organization_id")
    private Organization parentOrganization;

    @Setter
    @Builder.Default
    @Column(nullable = false)
    private Boolean canCreateCampaign = false;
}
