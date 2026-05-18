package org.example.backend.matching.model.evaluation;


import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Document(collection="evaluations")
public class EvaluationDocument {
    @Id
    private String id;

    @Indexed(unique=true)
    private String sessionId;

    private Long campaignIdx;  // 참조 관계는 조인 없이 ID 값만 유지합니다.
    private Long benefitIdx;
    private String companyName;

    private Evaluations evaluations; // 5개 평가 영역을 담는 내장 객체

    @CreatedDate
    private LocalDateTime startedAt; // @EnableMongoAuditing에 의해 작동

    @LastModifiedDate
    private LocalDateTime endedAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Evaluations {
        private Object customer;  // 기존의 CustomerEval Entity에 해당하던 POJO 객체
        private Object revenue;   // 기존의 RevenueEval
        private Object cost;      // 기존의 CostEval
        private Object operation; // 기존의 OperationEval
        private Object brand;     // 기존의 BrandEval
    }
}
