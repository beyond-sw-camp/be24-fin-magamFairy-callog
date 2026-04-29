package org.example.backend.campaign.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.common.model.BaseEntity;
import org.example.backend.matching.model.MarketingAsset;
import org.example.backend.organization.model.Organization;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "campaigns")
public class Campaign extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(nullable = false, length = 100)
    private String ownerLoginId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String purpose;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "campaign_tag", joinColumns = @JoinColumn(name = "campaign_idx"))
    @Column(name = "tag", length = 80)
    private List<String> tags = new ArrayList<>();

    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "campaign_partner", joinColumns = @JoinColumn(name = "campaign_idx"))
    @Column(name = "partner", length = 120)
    private List<String> partners = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String goals;

    @Column(columnDefinition = "TEXT")
    private String mainMessage;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "draft";

    @Column(nullable = false, length = 20)
    private String initials;

    @Column(nullable = false, length = 20)
    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private CampaignCategory category;

    @Column(precision = 15, scale = 2)
    private BigDecimal budget;

    @Column(length = 100)
    private String contactPerson;

    @Column(length = 100)
    private String contactEmail;

    @Column(length = 30)
    private String contactPhone;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(length = 100)
    private String approverId;

    private LocalDate approvalDate;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(columnDefinition = "TEXT")
    private String internalNotes;

    @Column(length = 100)
    private String templateSourceId;

    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CampaignParticipant> participants = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CampaignInvitation> invitations = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CampaignApprovalFlow> approvalFlows = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CampaignVersion> versions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CampaignComment> comments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CampaignContent> contents = new ArrayList<>();

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "campaign_marketing_asset",
            joinColumns = @JoinColumn(name = "campaign_id"),
            inverseJoinColumns = @JoinColumn(name = "asset_id")
    )
    private List<MarketingAsset> marketingAssets = new ArrayList<>();

    public void updateDetails(
            String name,
            String purpose,
            List<String> tags,
            LocalDate startDate,
            LocalDate endDate,
            List<String> partners,
            String goals,
            String mainMessage,
            String initials,
            String color
    ) {
        this.name = name;
        this.purpose = purpose;
        this.tags = new ArrayList<>(tags);
        this.startDate = startDate;
        this.endDate = endDate;
        this.partners.clear();
        this.partners.addAll(partners);
        this.goals = goals;
        this.mainMessage = mainMessage;
        this.initials = initials;
        this.color = color;
    }

    public void updateDetails(
            String name,
            String purpose,
            List<String> tags,
            LocalDate startDate,
            LocalDate endDate,
            List<String> partners,
            String goals,
            String mainMessage,
            String initials,
            String color,
            CampaignCategory category,
            BigDecimal budget,
            String contactPerson,
            String contactEmail,
            String contactPhone,
            RiskLevel riskLevel,
            String internalNotes
    ) {
        updateDetails(name, purpose, tags, startDate, endDate, partners, goals, mainMessage, initials, color);
        this.category = category;
        this.budget = budget;
        this.contactPerson = contactPerson;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.riskLevel = riskLevel == null ? RiskLevel.LOW : riskLevel;
        this.internalNotes = internalNotes;
    }

    public void updatePartners(List<String> partners) {
        this.partners = new ArrayList<>(partners);
    }

    public void updateStatus(String status) {
        this.status = status;
    }

    public void submitForApproval(String approverId) {
        this.status = "review";
        this.approvalStatus = ApprovalStatus.PENDING;
        this.approverId = approverId;
        this.approvalDate = null;
        this.rejectionReason = null;
    }

    public void markApproved(String approverId) {
        this.status = "live";
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.approverId = approverId;
        this.approvalDate = LocalDate.now();
        this.rejectionReason = null;
    }

    public void markRejected(String approverId, String rejectionReason) {
        this.status = "draft";
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.approverId = approverId;
        this.approvalDate = LocalDate.now();
        this.rejectionReason = rejectionReason;
    }

    public void updateAnalyticsMetadata(BigDecimal budget, RiskLevel riskLevel) {
        this.budget = budget;
        this.riskLevel = riskLevel == null ? RiskLevel.LOW : riskLevel;
    }
}
