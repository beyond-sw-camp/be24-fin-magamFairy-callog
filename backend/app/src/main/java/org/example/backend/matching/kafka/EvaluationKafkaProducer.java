package org.example.backend.matching.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.backend.matching.model.evaluation.EvaluationDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EvaluationKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.evaluation-start}")
    private String evaluationStartTopic;

    @Value("${app.kafka.topics.evaluation-collect}")
    private String evaluationCollectTopic;

    public void startEvaluation(EvaluationDto.StartEvaluation request) {
        send(evaluationStartTopic, request.getCampaignIdx(), request);
    }

    public void collect(EvaluationDto.CollectDto request) {
        send(evaluationCollectTopic, request.getUuid(), request);
    }

    private void send(String topic, String key, Object payload) {
        try {
            String message = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, message);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Kafka 메시지 변환에 실패했습니다.", e);
        }
    }
}