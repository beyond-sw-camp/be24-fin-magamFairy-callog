package org.example.backend.matching.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

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