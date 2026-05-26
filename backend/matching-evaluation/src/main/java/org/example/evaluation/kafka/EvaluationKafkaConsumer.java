package org.example.evaluation.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.evaluation.event.EvaluationCompletedEvent;
import org.example.evaluation.event.EvaluationCollectRequestedEvent;
import org.example.evaluation.event.EvaluationFailedEvent;
import org.example.evaluation.event.EvaluationStartRequestedEvent;
import org.example.evaluation.model.EvaluationDocument;
import org.example.evaluation.service.EvaluationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final EvaluationService evaluationService;
    private final EvaluationKafkaProducer evaluationKafkaProducer;

    @KafkaListener(
            topics = "${app.kafka.topics.evaluation-start}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeStartEvaluation(String message) {
        log.info("[Kafka] evaluation.start received: {}", message);

        EvaluationStartRequestedEvent event = null;
        try {
            event = objectMapper.readValue(message, EvaluationStartRequestedEvent.class);

            evaluationService.startEvaluation(event.toServiceRequest());
        } catch (JsonProcessingException e) {
            log.error("[Kafka] evaluation.start deserialize failed.", e);
            evaluationKafkaProducer.sendStartDeadLetter(null, message, e.getMessage());
        } catch (RuntimeException e) {
            log.error("[Kafka] evaluation.start processing failed.", e);
            evaluationKafkaProducer.sendFailed(EvaluationFailedEvent.builder()
                    .campaignPublicId(event != null ? event.getCampaignPublicId() : null)
                    .failedStage("N8N_WEBHOOK")
                    .reason(e.getMessage())
                    .build());
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.evaluation-collect}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeCollectEvaluation(String message) {
        log.info("[Kafka] evaluation.collect received: {}", message);

        try {
            EvaluationCollectRequestedEvent event =
                    objectMapper.readValue(message, EvaluationCollectRequestedEvent.class);

            EvaluationDocument saved = evaluationService.save(event);
            log.info("[Kafka] evaluation.collect saved. sessionId={}, resultId={}",
                    event.getSessionId(), saved.getId());

            evaluationKafkaProducer.sendCompleted(EvaluationCompletedEvent.builder()
                    .campaignPublicId(event.getCampaignPublicId())
                    .sessionId(event.getSessionId())
                    .resultId(saved.getId())
                    .build());
        } catch (JsonProcessingException e) {
            log.error("[Kafka] evaluation.collect deserialize failed.", e);
            evaluationKafkaProducer.sendCollectDeadLetter(null, message, e.getMessage());
        } catch (RuntimeException e) {
            log.error("[Kafka] evaluation.collect processing failed.", e);
            evaluationKafkaProducer.sendCollectDeadLetter(null, message, e.getMessage());
        }
    }
}
