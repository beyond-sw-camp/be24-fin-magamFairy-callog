package org.example.backend.matching.model.evaluation;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RevenueEval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;
    @CollectionTable(name = "revenue_improvement_directions", joinColumns = @JoinColumn(name = "eval_id"))
    @Column(name = "direction_text", columnDefinition = "TEXT") // 여기서 TEXT를 지정해야 함
    private List<String> improvementDirections;
    private Integer overallScore;

    @Column(columnDefinition = "TEXT")
    private String purchaseConversionProbability;
    @Column(columnDefinition = "TEXT")
    private String roomReservationIncreaseProbability;
    @Column(columnDefinition = "TEXT")
    private String appRegistrationIncreaseProbability;
    @Column(columnDefinition = "TEXT")
    private String membershipRegistrationRevisitProbability;
    @Column(columnDefinition = "TEXT")
    private String alignmentwithCampaignGoalsandKPIs;
}
