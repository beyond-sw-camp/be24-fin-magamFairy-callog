package org.example.evaluation.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.evaluation.model.EvaluationDto;
import org.example.evaluation.service.EvaluationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationKafkaConsumer {
    private final ObjectMapper objectMapper;
    private final EvaluationService evaluationService;

    @KafkaListener(
            topics = "${app.kafka.topics.evaluation-start}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeStartEvaluation(String message) {
        log.info("[Kafka] evaluation.start received: {}", message);

        try {
            EvaluationDto.StartEvaluationReq dto =
                    objectMapper.readValue(message, EvaluationDto.StartEvaluationReq.class);

            evaluationService.startEvaluation(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("evaluation.start 메시지 변환에 실패했습니다.", e);
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.evaluation-collect}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeCollectEvaluation(String message) {
        log.info("[Kafka] evaluation.collect received: {}", message);
    }
}