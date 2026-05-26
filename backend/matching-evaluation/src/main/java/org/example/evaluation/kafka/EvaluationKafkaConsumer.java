package org.example.evaluation.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.evaluation.event.EvaluationCompletedEvent;
import org.example.evaluation.event.EvaluationCollectRequestedEvent;
import org.example.evaluation.model.EvaluationDocument;
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
    private final EvaluationKafkaProducer evaluationKafkaProducer;

    /**
     * ⭐️ 메인 모듈(모놀리식)이 꽉 채워 보내준 데이터 스냅샷을 수신하는 리스너
     * 오직 모놀리식의 '응답 토픽'만 구독해야 합니다.
     */
    @KafkaListener(
            topics = "${app.kafka.topics.evaluation-start-reply:evaluation.start-reply}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeMainModuleResponse(EvaluationDto.StartEvaluation response) {
        log.info("[Evaluation MSA] 메인 모듈로부터 데이터 스냅샷 수신 완료. campaignIdx(publicId)={}",
                response.getCampaignIdx());

        // 수신한 데이터를 가지고 n8n 웹훅을 호출하는 비즈니스 로직 가동
        evaluationService.startEvaluation(response);
    }

    /**
     * n8n 연산 완료 후 결과 수집을 위한 기존 리스너 (기존 코드 유지)
     */
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