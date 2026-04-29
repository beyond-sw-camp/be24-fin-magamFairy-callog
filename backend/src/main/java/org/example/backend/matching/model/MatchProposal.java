package org.example.backend.matching.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.organization.model.Organization;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class MatchProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private MarketingAsset marketingAsset;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "benefit_id")
    private PartnerBenefits partnerBenefits;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "campaign")
    private Campaign campaign;

    private String targetCustomer;
    private String benefitSummary;
    private Integer totalScore;
    private String recommendationGrade;
    private String recommendationReason;
    private String status;
    private String evaluatedAt;
}
