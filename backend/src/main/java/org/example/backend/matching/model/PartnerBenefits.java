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
public class PartnerBenefits {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "affiliation_id")
    private Organization organization;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "campaign_idx")
    private Campaign campaign;

    private String name;
    private String type;
    private String scale;
    private String target;
    private String cost;
    private String status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
