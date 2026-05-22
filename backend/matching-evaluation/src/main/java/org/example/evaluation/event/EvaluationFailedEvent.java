package org.example.evaluation.event;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationFailedEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private String eventType = EvaluationEventType.EVALUATION_FAILED.name();

    @Builder.Default
    private String schemaVersion = "1.0";

    @Builder.Default
    private Instant occurredAt = Instant.now();

    private String campaignPublicId;
    private String sessionId;
    private String failedStage;
    private String reason;

    public String key() {
        return campaignPublicId != null ? campaignPublicId : sessionId;
    }
}
