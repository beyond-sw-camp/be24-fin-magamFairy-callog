package org.example.backend.matching.model.evaluation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import org.example.backend.matching.model.*;

import java.time.LocalDate;
import java.util.List;

public class EvaluationDto {

    @Getter
    @Builder
    public static class StartEvaluationReq {
        private Double dependency;
        private Long campaignIdx;
        private Long assetIdx;
        private Long benefitIdx;
        private Long goalIdx;
    }

    @Getter
    @Builder
    public static class StartEvaluation {
        private Double dependency; // 0.5와 같은 소수점을 처리하기 위해 Double 사용
        private Long campaignIdx;
        private MatchingDto.AssetRes asset;
        private MatchingDto.BenefitRes benefit;
        private CampaignGoalRes goal;

        @Getter
        @Builder
        public static class CampaignGoalRes {
            private String name;
            private GoalType primary;
            private GoalType secondary;
            private String kpiPrimary;
            private String kpiSecondary;
            private String budgetLimit;
            private String effortLimit;
            private LocalDate periodStart;
            private LocalDate periodEnd;
            private Integer weightRevenue;
            private Integer weightEffort;
            private Integer weightBrand;
            private String status;

            public static CampaignGoalRes toDto(CampaignGoal entity) {
                return CampaignGoalRes.builder()
                        .name(entity.getName())
                        .primary(entity.getPrimaryType())
                        .secondary(entity.getSecondaryType())
                        .kpiPrimary(entity.getKpiPrimary())
                        .kpiSecondary(entity.getKpiSecondary())
                        .budgetLimit(entity.getBudgetLimit())
                        .effortLimit(entity.getEffortLimit())
                        .periodStart(entity.getPeriodStart())
                        .periodEnd(entity.getPeriodEnd())
                        .weightRevenue(entity.getWeightRevenue())
                        .weightEffort(entity.getWeightEffort())
                        .weightBrand(entity.getWeightBrand())
                        .status(entity.getStatus())
                        .build();
            }
        }
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXISTING_PROPERTY, // 이미 존재하는 category 필드를 사용
            property = "category", // 구분자 필드명
            visible = true // DTO 객체 내부의 category 필드에도 값이 채워지도록 설정
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = CollectDto.Customer.class, name = "CUSTOMER"),
            @JsonSubTypes.Type(value = CollectDto.Revenue.class, name = "REVENUE"),
            @JsonSubTypes.Type(value = CollectDto.Cost.class, name = "COST"),
            @JsonSubTypes.Type(value = CollectDto.Operation.class, name = "OPERATION"),
            @JsonSubTypes.Type(value = CollectDto.Brand.class, name = "BRAND")
    })
    @Getter
    public static abstract class CollectDto {
        private String category;
        private Integer overallScore;
        private List<String> improvementDirections;
        private String uuid;
        private Long campaignIdx;

        @Getter
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Customer extends CollectDto {
            private String customerAgeGroup;
            private String customerSpendingPatterns;
            private String membershipTier;
            private String usageChannel;
            private String benefitCategory;
            public CustomerEval toEntity() {
                return CustomerEval.builder()
                        .customerAgeGroup(this.customerAgeGroup)
                        .customerSpendingPatterns(this.customerSpendingPatterns)
                        .membershipTier(this.membershipTier)
                        .usageChannel(this.usageChannel)
                        .benefitCategory(this.benefitCategory)
                        .overallScore(this.getOverallScore())
                        .improvementDirections(this.getImprovementDirections())
                        .build();
            }
        }

        @Getter
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Revenue extends CollectDto {
            private String purchaseConversionProbability;
            private String roomReservationIncreaseProbability;
            private String appRegistrationIncreaseProbability;
            private String membershipRegistrationRevisitProbability;
            private String alignmentwithCampaignGoalsandKPIs;
            public RevenueEval toEntity() {
                return RevenueEval.builder()
                        .purchaseConversionProbability(this.purchaseConversionProbability)
                        .roomReservationIncreaseProbability(this.roomReservationIncreaseProbability)
                        .appRegistrationIncreaseProbability(this.appRegistrationIncreaseProbability)
                        .membershipRegistrationRevisitProbability(this.membershipRegistrationRevisitProbability)
                        .alignmentwithCampaignGoalsandKPIs(this.alignmentwithCampaignGoalsandKPIs)
                        .overallScore(this.getOverallScore())
                        .improvementDirections(this.getImprovementDirections())
                        .build();
            }
        }

        @Getter
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Cost extends CollectDto {
            private String partnerSampleScale;
            private String partnerDiscountCostBurden;
            private String coProductionCostSharing;
            private String hanwhaDirectCostBurden;
            private String existingHanwhaChannelUtilization;
            public CostEval toEntity() {
                return CostEval.builder()
                        .partnerSampleScale(this.partnerSampleScale)
                        .partnerDiscountCostBurden(this.partnerDiscountCostBurden)
                        .coProductionCostSharing(this.coProductionCostSharing)
                        .hanwhaDirectCostBurden(this.hanwhaDirectCostBurden)
                        .existingHanwhaChannelUtilization(this.existingHanwhaChannelUtilization)
                        .overallScore(this.getOverallScore())
                        .improvementDirections(this.getImprovementDirections())
                        .build();
            }
        }

        @Getter
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Operation extends CollectDto {
            private String approvalStepsCount;
            private String legalReviewRequired;
            private String brandReviewRequired;
            private String deliverablesCount;
            private String participatingDeptsAndPartners;
            private String scheduleUrgency;
            private String offlineOrOnsiteStaffRequired;

            public OperationEval toEntity() {
                return OperationEval.builder()
                        .approvalStepsCount(this.approvalStepsCount)
                        .legalReviewRequired(this.legalReviewRequired)
                        .brandReviewRequired(this.brandReviewRequired)
                        .deliverablesCount(this.deliverablesCount)
                        .participatingDeptsAndPartners(this.participatingDeptsAndPartners)
                        .scheduleUrgency(this.scheduleUrgency)
                        .offlineOrOnsiteStaffRequired(this.offlineOrOnsiteStaffRequired)
                        .overallScore(this.getOverallScore())
                        .improvementDirections(this.getImprovementDirections())
                        .build();
            }
        }

        @Getter
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Brand extends CollectDto {
            private String brandTone;
            private String priceRange;
            private String customerExperience;
            private String brandTrust;
            private String reputationRisk;
            private String hanwhaImageConsistency;

            public BrandEval toEntity() {
                return BrandEval.builder()
                        .brandTone(this.brandTone)
                        .priceRange(this.priceRange)
                        .customerExperience(this.customerExperience)
                        .brandTrust(this.brandTrust)
                        .reputationRisk(this.reputationRisk)
                        .hanwhaImageConsistency(this.hanwhaImageConsistency)
                        .overallScore(this.getOverallScore())
                        .improvementDirections(this.getImprovementDirections())
                        .build();
            }
        }
    }


}
