package org.example.backend.matching.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.organization.model.Organization;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "campaign_idx")
    private Campaign campaign;

    private String target;
    private String type;
    private String scale;
    private String conditions;
    private Boolean isActive;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
