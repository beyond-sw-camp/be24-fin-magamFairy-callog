package org.example.backend.matching.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class EvaluationDto {

    @Getter
    @Builder
    public static class NewEvaluation {

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
