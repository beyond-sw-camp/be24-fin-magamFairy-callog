package org.example.backend.matching.model.evaluation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignDto;
import org.example.backend.matching.event.EvaluationStartRequestedEvent;
import org.example.backend.matching.model.*;
import org.example.backend.organization.model.Organization;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EvaluationDto {

    @Getter
    @Builder
    public static class MongoEvaluationRes {
        private String sessionId;
        private String goal;
        private String title;
        private String partner;
        private String assetDescription;
        private String offer;
        private String target;
        private EvaluationDocument.Evaluations evaluations;

        private LocalDateTime startedAt;
        private LocalDateTime endedAt;

        public static MongoEvaluationRes of(
                EvaluationDocument document
        ) {
            return MongoEvaluationRes.builder()
                    .sessionId(document.getSessionId())
                    .evaluations(document.getEvaluations())
                    .startedAt(document.getStartedAt())
                    .endedAt(document.getEndedAt())
                    .partner(document.getPartner())
                    .title(document.getTitle())
                    .goal(document.getGoal())
                    .assetDescription(document.getAssetDescription())
                    .offer(document.getOffer())
                    .target(document.getTarget())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class EvaluationRes {
        private String goal;
        private String title;
        private String partner;
        private String assetDescription;
        private String offer;
        private String target;
        private LocalDate startDate;
        private LocalDate endDate;
        private CustomerEvalDto customerEval;
        private RevenueEvalDto revenueEval;
        private BrandEvalDto brandEval;
        private OperationEvalDto operationEval;
        private CostEvalDto costEval;
        public static EvaluationRes toDto(Campaign campaign, PartnerBenefits benefits, Evaluation evaluation, String affiliate) {
            return EvaluationRes.builder()
                    .goal(campaign.getPrimaryGoal())
                    .title(benefits.getName())
                    .partner(affiliate)
                    .offer(benefits.getDescription())
                    .target(benefits.getTargetAudience())
                    .startDate(campaign.getStartDate())
                    .endDate(campaign.getEndDate())
                    .customerEval(CustomerEvalDto.from(evaluation.getCustomer()))
                    .revenueEval(RevenueEvalDto.from(evaluation.getRevenue()))
                    .brandEval(BrandEvalDto.from(evaluation.getBrand()))
                    .operationEval(OperationEvalDto.from(evaluation.getOperation()))
                    .costEval(CostEvalDto.from(evaluation.getCost()))
                    .build();

        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerEvalDto {
        private Long idx;
        private List<String> improvementDirections;
        private Integer overallScore;
        private String customerAgeGroup;
        private String customerSpendingPatterns;
        private String membershipTier;
        private String usageChannel;
        private String benefitCategory;

        public static CustomerEvalDto from(CustomerEval entity) {
            if (entity == null) return null;

            return CustomerEvalDto.builder()
                    .idx(entity.getIdx())
                    .improvementDirections(entity.getImprovementDirections() != null ?
                            new ArrayList<>(entity.getImprovementDirections()) : null)
                    .overallScore(entity.getOverallScore())
                    .customerAgeGroup(entity.getCustomerAgeGroup())
                    .customerSpendingPatterns(entity.getCustomerSpendingPatterns())
                    .membershipTier(entity.getMembershipTier())
                    .usageChannel(entity.getUsageChannel())
                    .benefitCategory(entity.getBenefitCategory())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenueEvalDto {
        private Long idx;
        private List<String> improvementDirections;
        private Integer overallScore;
        private String purchaseConversionProbability;
        private String roomReservationIncreaseProbability;
        private String appRegistrationIncreaseProbability;
        private String membershipRegistrationRevisitProbability;
        private String alignmentwithCampaignGoalsandKPIs;

        public static RevenueEvalDto from(RevenueEval entity) {
            if (entity == null) return null;

            return RevenueEvalDto.builder()
                    .idx(entity.getIdx())
                    .improvementDirections(entity.getImprovementDirections() != null ?
                            new ArrayList<>(entity.getImprovementDirections()) : null)
                    .overallScore(entity.getOverallScore())
                    .purchaseConversionProbability(entity.getPurchaseConversionProbability())
                    .roomReservationIncreaseProbability(entity.getRoomReservationIncreaseProbability())
                    .appRegistrationIncreaseProbability(entity.getAppRegistrationIncreaseProbability())
                    .membershipRegistrationRevisitProbability(entity.getMembershipRegistrationRevisitProbability())
                    .alignmentwithCampaignGoalsandKPIs(entity.getAlignmentwithCampaignGoalsandKPIs())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OperationEvalDto {
        private Long idx;
        private List<String> improvementDirections;
        private Integer overallScore;
        private String approvalStepsCount;
        private String legalReviewRequired;
        private String brandReviewRequired;
        private String deliverablesCount;
        private String participatingDeptsAndPartners;
        private String scheduleUrgency;
        private String offlineOrOnsiteStaffRequired;

        public static OperationEvalDto from(OperationEval entity) {
            if (entity == null) return null;
            return OperationEvalDto.builder()
                    .idx(entity.getIdx())
                    .improvementDirections(entity.getImprovementDirections() != null ?
                            new ArrayList<>(entity.getImprovementDirections()) : null)
                    .overallScore(entity.getOverallScore())
                    .approvalStepsCount(entity.getApprovalStepsCount())
                    .legalReviewRequired(entity.getLegalReviewRequired())
                    .brandReviewRequired(entity.getBrandReviewRequired())
                    .deliverablesCount(entity.getDeliverablesCount())
                    .participatingDeptsAndPartners(entity.getParticipatingDeptsAndPartners())
                    .scheduleUrgency(entity.getScheduleUrgency())
                    .offlineOrOnsiteStaffRequired(entity.getOfflineOrOnsiteStaffRequired())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class StartEvaluationReq {
        private Long benefitIdx;
        private String publicId;
        public static StartEvaluationReq from(EvaluationStartRequestedEvent event) {
            return StartEvaluationReq.builder().benefitIdx(event.getBenefitIdx()).publicId(event.getCampaignPublicId()).build();
        }
    }

    @Getter
    @Builder
    public static class StartEvaluation {
        private String campaignIdx;
        private CampaignDto.Res campaign;
        private MatchingDto.BenefitRes benefit;

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

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BrandEvalDto {
        private Long idx;
        private List<String> improvementDirections;
        private Integer overallScore;
        private String brandTone;
        private String priceRange;
        private String customerExperience;
        private String brandTrust;
        private String reputationRisk;
        private String hanwhaImageConsistency;

        public static BrandEvalDto from(BrandEval entity) {
            if (entity == null) return null;

            return BrandEvalDto.builder()
                    .idx(entity.getIdx())
                    .improvementDirections(entity.getImprovementDirections() != null ?
                            new ArrayList<>(entity.getImprovementDirections()) : null)
                    .overallScore(entity.getOverallScore())
                    .brandTone(entity.getBrandTone())
                    .priceRange(entity.getPriceRange())
                    .customerExperience(entity.getCustomerExperience())
                    .brandTrust(entity.getBrandTrust())
                    .reputationRisk(entity.getReputationRisk())
                    .hanwhaImageConsistency(entity.getHanwhaImageConsistency())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CostEvalDto {
        private Long idx;
        private List<String> improvementDirections;
        private Integer overallScore;
        private String partnerSampleScale;
        private String partnerDiscountCostBurden;
        private String coProductionCostSharing;
        private String hanwhaDirectCostBurden;
        private String existingHanwhaChannelUtilization;
        public static CostEvalDto from(CostEval entity) {
            if (entity == null) return null;

            return CostEvalDto.builder()
                    .idx(entity.getIdx())
                    .improvementDirections(entity.getImprovementDirections() != null ?
                            new ArrayList<>(entity.getImprovementDirections()) : null)
                    .overallScore(entity.getOverallScore())
                    .partnerSampleScale(entity.getPartnerSampleScale())
                    .partnerDiscountCostBurden(entity.getPartnerDiscountCostBurden())
                    .coProductionCostSharing(entity.getCoProductionCostSharing())
                    .hanwhaDirectCostBurden(entity.getHanwhaDirectCostBurden())
                    .existingHanwhaChannelUtilization(entity.getExistingHanwhaChannelUtilization())
                    .build();
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
        private String campaignIdx;
        private Long benefitIdx;

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
