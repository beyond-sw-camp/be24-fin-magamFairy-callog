package org.example.evaluation.event;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.evaluation.model.EvaluationDto;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvaluationStartRequestedEvent {

    @JsonAlias({"campaignIdx", "campaignPublicId"})
    private String campaignPublicId;

    private Object campaign;
    private Object benefit;

    public String key() {
        return campaignPublicId;
    }

    public EvaluationDto.StartEvaluationReq toServiceRequest() {
        return EvaluationDto.StartEvaluationReq.builder()
                .campaignIdx(campaignPublicId)
                .campaign(campaign)
                .benefit(benefit)
                .build();
    }
}
