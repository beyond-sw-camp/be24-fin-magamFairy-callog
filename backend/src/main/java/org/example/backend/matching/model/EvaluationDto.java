package org.example.backend.matching.model;

import lombok.*;

import java.util.List;

public class EvaluationDto {

    @Getter
    @Builder
    public static class StartEvaluationReq {
        private String kpi;
        private Double dependency;
        private Long assetIdx;
        private Long benefitIdx;
    }

    @Getter
    @Builder
    public static class StartEvaluation {
        private String kpi;
        private Double dependency; // 0.5와 같은 소수점을 처리하기 위해 Double 사용
        private AssetRes asset;
        private BenefitRes benefit;

        @Getter
        @Builder
        public static class AssetRes {
            private String type;
            private String target;
            private String conditions;
            private String scale;
            public static AssetRes toDto(MarketingAsset entity){
                return AssetRes.builder()
                        .type(entity.getType())
                        .target(entity.getTarget())
                        .conditions(entity.getConditions())
                        .scale(entity.getScale())
                        .build();
            }
        }

        @Getter
        @Builder
        public static class BenefitRes {
            private String cost;
            private String name;
            private String scale;
            private String target;
            private String type;
            public static BenefitRes toDto(PartnerBenefits entity){
                return BenefitRes.builder()
                        .cost(entity.getCost())
                        .name(entity.getName())
                        .scale(entity.getScale())
                        .target(entity.getTarget())
                        .type(entity.getType())
                        .build();
            }
        }
    }

    @Getter
    @Builder
    public static class Collect {
        private String token;
        private Customer customer;
        private Revenue revenue;
        private Operation operation;
        private Cost cost;
        private Brand brand;
    }
    @Getter
    @NoArgsConstructor
    public static class Customer {
        private String customerAgeGroup;
        private String customerSpendingPatterns;
        private String membershipTier;
        private String usageChannel;
        private String benefitCategory;
        private int overallScore;
        private List<String> improvementDirections;
    }

    @Getter
    @NoArgsConstructor
    public static class Revenue {
        private String purchaseConversionProbability;
        private String roomReservationIncreaseProbability;
        private String appRegistrationIncreaseProbability;
        private String membershipRegistrationRevisitProbability;
        private String alignmentwithCampaignGoalsandKPIs;
        private int overallScore;
        private List<String> improvementDirections;
    }

    @Getter
    @NoArgsConstructor
    public static class Cost {
        private String partnerSampleScale;
        private String partnerDiscountCostBurden;
        private String coProductionCostSharing;
        private String hanwhaDirectCostBurden;
        private String existingHanwhaChannelUtilization;
        private int overallScore;
        private List<String> improvementDirections;
    }

    @Getter
    @NoArgsConstructor
    public static class Operation {
        private String approvalStepsCount;
        private String legalReviewRequired;
        private String brandReviewRequired;
        private String deliverablesCount;
        private String participatingDeptsAndPartners;
        private String scheduleUrgency;
        private String offlineOrOnsiteStaffRequired;
        private int overallScore;
        private List<String> improvementDirections;
    }

    @Getter
    @NoArgsConstructor
    public static class Brand {
        private String brandTone;
        private String priceRange;
        private String customerExperience;
        private String brandTrust;
        private String reputationRisk;
        private String hanwhaImageConsistency;
        private int overallScore;
        private List<String> improvementDirections;
    }


    @Getter
    @Builder
    public static class EvaluationRes{
        // 점수
        private Scores scores;
        // 상세 데이터
        private String benefitSummary;
        private String reason;
        private String warnings;
        private String kpis;
        private String evidence;
        private String nextActions;
        private String manualScore;

        @Builder
        @Getter
        private static class Scores{
            private Integer customerFit;
            private Integer revenue;
            private Integer cost;
            private Integer operation;
            private Integer brand;
        }

        public static EvaluationRes toDto(Evaluation entity) {
            return EvaluationRes.builder()
                    .scores(Scores.builder() // 여기서 Scores 객체를 생성
                            .customerFit(entity.getCustomerFit())
                            .revenue(entity.getRevenue())
                            .cost(entity.getCost())
                            .operation(entity.getOperation())
                            .brand(entity.getBrand())
                            .build())
                    .benefitSummary(entity.getBenefitSummary())
                    .reason(entity.getReason())
                    .warnings(entity.getWarnings())
                    .kpis(entity.getKpis())
                    .evidence(entity.getEvidence())
                    .nextActions(entity.getNextActions())
                    .manualScore(entity.getManualScore())
                    .build();
        }
    }


}
