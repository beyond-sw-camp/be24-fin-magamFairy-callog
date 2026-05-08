package org.example.backend.campaign.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.common.model.BaseEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Column(name = "public_id", unique = true, length = 36)
    private String publicId;

    @jakarta.persistence.PrePersist
    protected void onPrePersist() {
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID().toString();
        }
    }

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
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<String> tags = new ArrayList<>();

    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "campaign_partner", joinColumns = @JoinColumn(name = "campaign_idx"))
    @Column(name = "partner", length = 120)
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<String> partners = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String goals;

    @Column(columnDefinition = "TEXT")
    private String mainMessage;

    @Column(length = 160)
    private String assetName;

    @Column(columnDefinition = "TEXT")
    private String assetDescription;

    @Column(length = 120)
    private String primaryGoal;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "campaign_method", joinColumns = @JoinColumn(name = "campaign_idx"))
    @Column(name = "method", length = 120)
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<String> campaignMethods = new ArrayList<>();

    @Column(length = 80)
    private String maxCost;

    @Column(length = 80)
    private String minRevenue;

    @Column(length = 80)
    private String ownerName;

    @Column(length = 160)
    private String ownerEmail;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "draft";

    @Column(columnDefinition = "TEXT")
    private String kpiAnalysis;

    public void updateKpiAnalysis(String kpiAnalysis) {
        this.kpiAnalysis = kpiAnalysis;
    }

    public boolean isClosed() {
        return "closed".equals(this.status);
    }

    @Column(nullable = false, length = 20)
    private String initials;

    @Column(length = 40)
    private String icon;

    @Column(nullable = false, length = 20)
    private String color;

    /**
     * 캠페인 썸네일 S3 object key. null이면 디렉토리 카드는 단색+이니셜 fallback.
     * Phase 4에서 AI 자동 생성으로 채울 수도 있음.
     */
    @Column(name = "thumbnail_object_key", length = 500)
    private String thumbnailObjectKey;

    public void updateThumbnailObjectKey(String objectKey) {
        this.thumbnailObjectKey = objectKey;
    }

    /**
     * 캠페인 소개 페이지 공개 범위.
     * PRIVATE / HQ_ONLY / HQ_AND_AFFILIATE / AFFILIATE_ONLY / EXTERNAL_ONLY / ALL
     */
    @Builder.Default
    @Column(name = "visibility", nullable = false, length = 30)
    private String visibility = "PRIVATE";

    public void updateVisibility(String visibility) {
        this.visibility = visibility == null ? "PRIVATE" : visibility;
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
            String assetName,
            String assetDescription,
            String primaryGoal,
            List<String> campaignMethods,
            String maxCost,
            String minRevenue,
            String ownerName,
            String ownerEmail,
            String initials,
            String icon,
            String color
    ) {
        this.name = name;
        this.purpose = purpose;
        this.tags.clear();
        this.tags.addAll(tags);
        this.startDate = startDate;
        this.endDate = endDate;
        this.partners.clear();
        this.partners.addAll(partners);
        this.goals = goals;
        this.mainMessage = mainMessage;
        this.assetName = assetName;
        this.assetDescription = assetDescription;
        this.primaryGoal = primaryGoal;
        this.campaignMethods.clear();
        this.campaignMethods.addAll(campaignMethods);
        this.maxCost = maxCost;
        this.minRevenue = minRevenue;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
        this.initials = initials;
        this.icon = icon;
        this.color = color;
    }

    public void updatePartners(List<String> partners) {
        this.partners.clear();
        this.partners.addAll(partners);
    }

    public void updateStatus(String status) {
        this.status = status;
    }
}
