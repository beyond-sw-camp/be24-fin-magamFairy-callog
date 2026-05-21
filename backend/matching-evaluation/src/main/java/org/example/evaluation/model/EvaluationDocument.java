package org.example.evaluation.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;


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

    private String campaignIdx;  // campaign publicId 문자열을 저장합니다.
    private Long benefitIdx;

    private String goal;
    private String title;
    private String partner;
    private String assetDescription;
    private String offer;
    private String target; // 다른 테이블에서 조인해올 부가정보들

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
        private Customer customer;
        private Revenue revenue;
        private Cost cost;
        private Operation operation;
        private Brand brand;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Customer {
        private List<String> improvementDirections;
        private Integer overallScore;
        private String customerAgeGroup;
        private String customerSpendingPatterns;
        private String membershipTier;
        private String usageChannel;
        private String benefitCategory;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Revenue {
        private List<String> improvementDirections;
        private Integer overallScore;
        private String purchaseConversionProbability;
        private String roomReservationIncreaseProbability;
        private String appRegistrationIncreaseProbability;
        private String membershipRegistrationRevisitProbability;
        private String alignmentwithCampaignGoalsandKPIs;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cost {
        private List<String> improvementDirections;
        private Integer overallScore;
        private String partnerSampleScale;
        private String partnerDiscountCostBurden;
        private String coProductionCostSharing;
        private String hanwhaDirectCostBurden;
        private String existingHanwhaChannelUtilization;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Operation {
        private List<String> improvementDirections;
        private Integer overallScore;
        private String approvalStepsCount;
        private String legalReviewRequired;
        private String brandReviewRequired;
        private String deliverablesCount;
        private String participatingDeptsAndPartners;
        private String scheduleUrgency;
        private String offlineOrOnsiteStaffRequired;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Brand {
        private List<String> improvementDirections;
        private Integer overallScore;
        private String brandTone;
        private String priceRange;
        private String customerExperience;
        private String brandTrust;
        private String reputationRisk;
        private String hanwhaImageConsistency;
    }
}
