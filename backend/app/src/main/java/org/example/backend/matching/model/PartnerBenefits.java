package org.example.backend.matching.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.organization.model.Organization;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affiliation_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_idx")
    private Campaign campaign;

    // [기본 정보]
    private String name;
    private String type;

    @Column(columnDefinition = "TEXT")
    private String description;

    // [규모·재고]
    private Long quantity;
    private String quantityUnit;
    private Long valuePerPerson;
    private Long totalValue;

    // [기간]
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Boolean alwaysNegotiable;
    private Integer prepDays;

    // [대상]
    private String targetAudience;
    private Long expectedReach;

    // [비용 부담]
    private String costBearer;
    private Integer costPartnerPercent;
    private Integer costOursPercent;

    @Column(columnDefinition = "TEXT")
    private String costDetails;

    // [운영 조건]
    @Column(columnDefinition = "TEXT")
    private String exposureChannels;

    @Column(columnDefinition = "TEXT")
    private String requiredCollaborations;

    @Column(columnDefinition = "TEXT")
    private String conditions;

    // [연결 자산]
    @Column(columnDefinition = "TEXT")
    private String desiredAssets;
    private Boolean autoRecommend;

    // [담당자]
    private String managerName;
    private String managerEmail;
    private String managerPhone;

    // 상태 및 생성일
    private String status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}