package org.example.backend.matching.model.evaluation;

import jakarta.persistence.*;
import lombok.*;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.matching.model.PartnerBenefits;
import org.example.backend.organization.model.Organization;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class) // 시간 자동 기록을 위한 리스너
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(unique = true, nullable = false, updatable = false)
    private String sessionId; // n8n에서 발급한 UUID를 담을 필드

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_idx", unique = false)
    private Campaign campaign;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "customer_idx", unique = true)
    private CustomerEval customer;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "revenue_idx", unique = true)
    private RevenueEval revenue;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "cost_idx", unique = true)
    private CostEval cost;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "operation_idx", unique = true)
    private OperationEval operation;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "brand_idx", unique = true)
    private BrandEval brand;

    // 평가 시작 시간 (레코드 생성 시 자동 기록)
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime startedAt;

    // 평가 종료 시간 (최종 수정 시 자동 업데이트)
    @LastModifiedDate
    private LocalDateTime endedAt;

    public Evaluation(String sessionId) {
        this.sessionId = sessionId;
    }

    public void updateEval(Object eval, String category) {
        switch (category) {
            case "CUSTOMER" -> this.customer = (CustomerEval) eval;
            case "REVENUE" -> this.revenue = (RevenueEval) eval;
            case "COST" -> this.cost = (CostEval) eval;
            case "OPERATION" -> this.operation = (OperationEval) eval;
            case "BRAND" -> this.brand = (BrandEval) eval;
        }
    }
}
