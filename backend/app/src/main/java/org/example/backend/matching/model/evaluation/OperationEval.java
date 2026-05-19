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
public class OperationEval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;
    @ElementCollection
    @CollectionTable(name = "operation_improvement_directions", joinColumns = @JoinColumn(name = "eval_id"))
    @Column(name = "direction_text", columnDefinition = "TEXT") // 여기서 TEXT를 지정해야 함
    private List<String> improvementDirections;
    private Integer overallScore;

    @Column(columnDefinition = "TEXT")
    private String approvalStepsCount;
    @Column(columnDefinition = "TEXT")
    private String legalReviewRequired;
    @Column(columnDefinition = "TEXT")
    private String brandReviewRequired;
    @Column(columnDefinition = "TEXT")
    private String deliverablesCount;
    @Column(columnDefinition = "TEXT")
    private String participatingDeptsAndPartners;
    @Column(columnDefinition = "TEXT")
    private String scheduleUrgency;
    @Column(columnDefinition = "TEXT")
    private String offlineOrOnsiteStaffRequired;
}
