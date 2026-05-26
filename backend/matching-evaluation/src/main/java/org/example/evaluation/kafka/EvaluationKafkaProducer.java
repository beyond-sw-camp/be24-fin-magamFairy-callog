package org.example.evaluation.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.evaluation.event.EvaluationCompletedEvent;
import org.example.evaluation.event.EvaluationCollectRequestedEvent;
import org.example.evaluation.event.EvaluationDeadLetterEvent;
import org.example.evaluation.event.EvaluationEventType;
import org.example.evaluation.event.EvaluationFailedEvent;
import org.example.evaluation.event.EvaluationStartRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EvaluationKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.evaluation-start:evaluation.start}")
    private String evaluationStartTopic;

    @Value("${app.kafka.topics.evaluation-collect:evaluation.collect}")
    private String evaluationCollectTopic;

    @Value("${app.kafka.topics.evaluation-completed:evaluation.completed}")
    private String evaluationCompletedTopic;

    @Value("${app.kafka.topics.evaluation-failed:evaluation.failed}")
    private String evaluationFailedTopic;

    @Value("${app.kafka.topics.evaluation-start-dlq:evaluation.start.dlq}")
    private String evaluationStartDlqTopic;

    @Value("${app.kafka.topics.evaluation-collect-dlq:evaluation.collect.dlq}")
    private String evaluationCollectDlqTopic;

//    public void sendStart(EvaluationStartRequestedEvent event) {
//        send(evaluationStartTopic, event.key(), event);
//    }

    public void sendCollect(EvaluationCollectRequestedEvent event) {
        send(evaluationCollectTopic, event.key(), event);
    }

    public void sendCompleted(EvaluationCompletedEvent event) {
        send(evaluationCompletedTopic, event.key(), event);
    }

    public void sendFailed(EvaluationFailedEvent event) {
        send(evaluationFailedTopic, event.key(), event);
    }

    public void sendStartDeadLetter(String key, String rawPayload, String reason) {
        sendDeadLetter(evaluationStartDlqTopic, EvaluationEventType.EVALUATION_START_DEAD_LETTER, key, rawPayload, reason);
    }

    public void sendCollectDeadLetter(String key, String rawPayload, String reason) {
        sendDeadLetter(evaluationCollectDlqTopic, EvaluationEventType.EVALUATION_COLLECT_DEAD_LETTER, key, rawPayload, reason);
    }

    private void sendDeadLetter(String topic, EvaluationEventType eventType, String key, String rawPayload, String reason) {
        EvaluationDeadLetterEvent event = EvaluationDeadLetterEvent.builder()
                .eventType(eventType.name())
                .sourceTopic(topic.replace(".dlq", ""))
                .key(key)
                .rawPayload(rawPayload)
                .reason(reason)
                .build();

        send(topic, key, event);
    }

    private void send(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize Kafka event.", e);
        }
    }
}
