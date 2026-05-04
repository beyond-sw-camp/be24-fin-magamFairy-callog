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
public class BrandEval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;
    @CollectionTable(name = "improvement_directions", joinColumns = @JoinColumn(name = "eval_id"))
    @Column(name = "direction_text", columnDefinition = "TEXT") // 여기서 TEXT를 지정해야 함
    private List<String> improvementDirections;
    private Integer overallScore;

    @Column(columnDefinition = "TEXT")
    private String brandTone;
    @Column(columnDefinition = "TEXT")
    private String priceRange;
    @Column(columnDefinition = "TEXT")
    private String customerExperience;
    @Column(columnDefinition = "TEXT")
    private String brandTrust;
    @Column(columnDefinition = "TEXT")
    private String reputationRisk;
    @Column(columnDefinition = "TEXT")
    private String hanwhaImageConsistency;
}
