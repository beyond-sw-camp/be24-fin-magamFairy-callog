package org.example.evaluation.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

import java.time.LocalDateTime;

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
    }
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SaveEvaluationReq {

        @JsonAlias("uuid")
        private String sessionId;

        private Long campaignIdx;
        private Long benefitIdx;

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
                    .campaignIdx(campaignIdx)
                    .benefitIdx(benefitIdx)
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
}
