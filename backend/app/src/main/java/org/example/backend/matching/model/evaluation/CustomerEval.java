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
public class CustomerEval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ElementCollection
    @CollectionTable(name = "customer_improvement_directions", joinColumns = @JoinColumn(name = "eval_id"))
    @Column(name = "direction_text", columnDefinition = "TEXT") // 여기서 TEXT를 지정해야 함
    private List<String> improvementDirections;
    private Integer overallScore;

    @Column(columnDefinition = "TEXT")
    private String customerAgeGroup;
    @Column(columnDefinition = "TEXT")
    private String customerSpendingPatterns;
    @Column(columnDefinition = "TEXT")
    private String membershipTier;
    @Column(columnDefinition = "TEXT")
    private String usageChannel;
    @Column(columnDefinition = "TEXT")
    private String benefitCategory;

}
