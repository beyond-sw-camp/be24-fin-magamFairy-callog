package org.example.backend.matching.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.organization.model.Organization;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class MarketingAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "affiliate_idx")
    private Organization organization;

    private String target;
    private String type;
    private String scale;
    private String conditions;
    private Boolean isActive;
    private String createdAt;
}
