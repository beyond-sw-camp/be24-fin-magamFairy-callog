package org.example.backend.adcheck.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.adcheck.service.AdAiAnalysisService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AiJudgeKafkaEventListener {
    private final ObjectMapper objectMapper;
    private final AdAiAnalysisService adAiAnalysisService;

    public AiJudgeKafkaEventListener(ObjectMapper objectMapper, AdAiAnalysisService adAiAnalysisService) {
        this.objectMapper = objectMapper;
        this.adAiAnalysisService = adAiAnalysisService;
    }

    @KafkaListener(
            topics = "${ai-judge.kafka.completed-topic:ai-judge.completed}",
            groupId = "${spring.kafka.consumer.group-id:callog-app-ai-judge}",
            autoStartup = "${ai-judge.kafka.enabled:false}"
    )
    public void handleAiJudgeCompleted(String payload) {
        try {
            JsonNode event = objectMapper.readTree(payload);
            log.info(
                    "Consumed ai-judge Kafka event. eventType={}, analysisJobId={}, aiStatus={}, fileName={}",
                    text(event, "eventType"),
                    text(event, "analysisJobId"),
                    text(event, "aiStatus"),
                    text(event, "fileName")
            );
            adAiAnalysisService.applyAiJudgeEvent(event);
        } catch (Exception e) {
            log.warn("AI judge Kafka event cannot be parsed. payload={}", payload, e);
        }
    }

    private String text(JsonNode node, String name) {
        JsonNode value = node == null ? null : node.path(name);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return "";
        }
        return value.asText("");
    }
}
