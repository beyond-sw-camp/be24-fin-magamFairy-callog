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
public class Evaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Organization organization;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "campaign_idx")
    private Campaign campaign;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "benefits_idx")
    private PartnerBenefits benefits;

    // 점수
    private Integer customerFit;
    private Integer revenue;
    private Integer cost;
    private Integer operation;
    private Integer brand;
    // 상세 데이터
    private String benefitSummary;
    private String reason;
    private String warnings;
    private String kpis;
    private String evidence;
    private String nextActions;
    private String manualScore;


    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime evaluationDate;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime lastUpdate;
}
