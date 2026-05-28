package org.example.evaluation.model;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class N8nEvaluationPayloadDto {

    // 공통 메타 데이터 [cite: 1, 6, 11, 16, 21]
    private String uuid;
    private String publicId;
    private Long campaignIdx;
    private Long benefitIdx;
    private String category;
    private Integer overallScore;
    private List<String> improvementDirections;

    // BRAND 영역 필드 [cite: 1]
    private String brandTone;
    private String priceRange;
    private String customerExperience;
    private String brandTrust;
    private String reputationRisk;
    private String hanwhaImageConsistency;

    // COST 영역 필드 [cite: 6]
    private String partnerSampleScale;
    private String partnerDiscountCostBurden;
    private String coProductionCostSharing;
    private String hanwhaDirectCostBurden;
    private String existingHanwhaChannelUtilization;

    // CUSTOMER 영역 필드 [cite: 11]
    private String customerAgeGroup;
    private String customerSpendingPatterns;
    private String membershipTier;
    private String usageChannel;
    private String benefitCategory;

    // OPERATION 영역 필드 [cite: 16]
    private String approvalStepsCount;
    private String legalReviewRequired;
    private String brandReviewRequired;
    private String deliverablesCount;
    private String participatingDeptsAndPartners;
    private String scheduleUrgency;
    private String offlineOrOnsiteStaffRequired;

    // REVENUE 영역 필드 [cite: 21]
    private String purchaseConversionProbability;
    private String roomReservationIncreaseProbability;
    private String appRegistrationIncreaseProbability;
    private String membershipRegistrationRevisitProbability;
    private String alignmentwithCampaignGoalsandKPIs;
}