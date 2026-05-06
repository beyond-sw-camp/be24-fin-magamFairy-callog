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
import java.util.List;

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
    @JoinColumn(name = "owner_idx")
    private Organization organization;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "campaign_idx")
    private Campaign campaign;

    private String affiliate;
    private List<String> blockedPartners;
    private String category;
    private String conditions;
    private String customAffiliate;
    private String exposureValue;
    private String matchingStatus;
    private List<String> partnerFit;
    private String performance;
    private String publicStatus;
    private String scale;
    private String supplyLimit;
    private String target;
    private String type;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    public void update(String target, String type, String scale, String conditions) {
        this.target = target;
        this.type = type;
        this.scale = scale;
        this.conditions = conditions;
    }
}
