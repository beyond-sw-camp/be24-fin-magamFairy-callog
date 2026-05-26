package org.example.evaluation.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
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

        public static MongoEvaluationRes of(EvaluationDocument document) {
            return MongoEvaluationRes.builder()
                    .sessionId(document.getSessionId())
                    .goal(document.getGoal())
                    .title(document.getTitle())
                    .partner(document.getPartner())
                    .assetDescription(document.getAssetDescription())
                    .offer(document.getOffer())
                    .target(document.getTarget())
                    .evaluations(document.getEvaluations())
                    .startedAt(document.getStartedAt())
                    .endedAt(document.getEndedAt())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StartEvaluationReq {
        private Long benefitIdx;
        private String campaignIdx;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SaveEvaluationReq {

        @JsonAlias("uuid")
        private String sessionId;
        @JsonAlias({"campaignIdx", "campaignPublicId"})
        private String publicId;
        private String goal;
        private String title;
        private String partner;
        private String assetDescription;
        private String offer;
        private String target;

        private EvaluationDocument.Evaluations evaluations;

        public EvaluationDocument toDocument() {
            return EvaluationDocument.builder()
                    .sessionId(sessionId)
                    .publicId(publicId)
                    .goal(goal)
                    .title(title)
                    .partner(partner)
                    .assetDescription(assetDescription)
                    .offer(offer)
                    .target(target)
                    .evaluations(evaluations)
                    .build();
        }
    }

    // kafka용 Dto
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class StartEvaluation {
        private String campaignIdx; // 내용물은 String publicId
        private CampaignRes campaign; // Object 대신 정석대로 정적 타입 매핑
        private BenefitRes benefit;   // 모놀리식의 Benefit 스펙 매핑
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class CampaignRes {
        private String id;
        private Long idx;
        private String name;
        private String purpose;
        private List<String> tags;
        private LocalDate startDate;
        private LocalDate endDate;
        private String period;
        private List<String> partners;
        private String goals;
        private String assetName;
        private String assetDescription;
        private String primaryGoal;
        private List<String> campaignMethods;
        private String maxCost;
        private String minRevenue;
        private String status;
        private String initials;
        private String icon;
        private String color;
        private Date createdAt;
        private Date updatedAt;

        // 💡 의존성 제거를 위해 String으로 안전하게 수신합니다.
        private String myCampaignRole;
        private boolean organizationIsPm;
        private Integer totalTaskCount;
    }

    /**
     * ⭐️ 모놀리식의 Benefit 클래스 스펙과 1:1 매핑되는 클래스
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class BenefitRes {
        private Long idx;
        private String affiliate;
        private Long campaignIdx;

        // [기본 정보]
        private String name;
        private String type;
        private String description;

        // [규모·재고]
        private Long quantity;
        private String quantityUnit;
        private Long valuePerPerson;
        private Long totalValue;

        // [기간]
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private Boolean alwaysNegotiable;
        private Integer prepDays;

        // [대상]
        private String targetAudience;
        private Long expectedReach;

        // [비용 부담]
        private String costBearer;
        private Integer costPartnerPercent;
        private Integer costOursPercent;
        private String costDetails;

        // [운영 조건]
        private String exposureChannels;
        private String requiredCollaborations;
        private String conditions;

        // [연결 자산]
        private String desiredAssets;
        private Boolean autoRecommend;

        // [담당자]
        private String managerName;
        private String managerEmail;
        private String managerPhone;

        // 상태 및 생성일
        private String status;
        private LocalDateTime createdAt;
    }
}
