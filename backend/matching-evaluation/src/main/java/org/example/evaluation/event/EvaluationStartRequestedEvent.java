package org.example.evaluation.event;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.evaluation.model.EvaluationDto;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvaluationStartRequestedEvent {

    private String campaignPublicId;
    private Long benefitIdx;

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private long timestamp = System.currentTimeMillis();
}
