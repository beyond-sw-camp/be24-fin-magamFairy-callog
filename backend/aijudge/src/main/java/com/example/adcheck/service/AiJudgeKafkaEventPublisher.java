package com.example.adcheck.service;

import com.example.adcheck.model.AdCheckDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class AiJudgeKafkaEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai-judge.kafka.enabled:false}")
    private boolean enabled;

    @Value("${ai-judge.kafka.completed-topic:ai-judge.completed}")
    private String completedTopic;

    public AiJudgeKafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishCompleted(AdCheckDto.FileCheckRes response) {
        publish("AI_JUDGE_COMPLETED", response);
    }

    public void publishFailed(AdCheckDto.FileCheckRes response) {
        publish("AI_JUDGE_FAILED", response);
    }

    private void publish(String eventType, AdCheckDto.FileCheckRes response) {
        if (!enabled || response == null) {
            return;
        }

        try {
            String key = response.getAnalysisJobId();
            String payload = objectMapper.writeValueAsString(toEvent(eventType, response));
            kafkaTemplate.send(completedTopic, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish ai-judge event. topic={}, analysisJobId={}, eventType={}",
                                    completedTopic, key, eventType, ex);
                            return;
                        }
                        log.info("Published ai-judge event. topic={}, analysisJobId={}, eventType={}",
                                completedTopic, key, eventType);
                    });
        } catch (Exception e) {
            log.warn("AI judge Kafka event cannot be serialized. analysisJobId={}, eventType={}",
                    response.getAnalysisJobId(), eventType, e);
        }
    }

    private Map<String, Object> toEvent(String eventType, AdCheckDto.FileCheckRes response) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", eventType);
        event.put("occurredAt", Instant.now().toString());
        event.put("analysisJobId", response.getAnalysisJobId());
        event.put("analysisObjectPrefix", response.getAnalysisObjectPrefix());
        event.put("fileName", response.getFileName());
        event.put("fileObjectKey", response.getFileObjectKey());
        event.put("fileContentType", response.getFileContentType());
        event.put("fileSize", response.getFileSize());
        event.put("aiStatus", response.getStatus());
        event.put("law", response.getLaw());
        event.put("violationText", response.getViolationText());
        event.put("reason", response.getReason());
        event.put("suggestion", response.getSuggestion());
        event.put("extractionMode", response.getExtractionMode());
        event.put("finalResultObjectKey", response.getFinalResultObjectKey());
        event.put("errorMessage", response.getErrorMessage());
        event.put("context", response.getContext());
        return event;
    }
}
