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

    @KafkaListener(
            topics = "${app.kafka.topics.evaluation-start}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @KafkaListener(topics = "campaign-response-topic", groupId = "evaluation-module-group")
    public void consumeMainModuleResponse(EvaluationDto.StartEvaluation response) {
        log.info("[Evaluation MSA] 메인 모듈로부터 데이터 스냅샷 수신 완료. campaignIdx(publicId)={}",
                response.getCampaignIdx());

        // 2. 수신한 꽉 찬 데이터를 가지고 n8n 웰훅을 찌르거나 파이프라인 가동!
        // 기존 startEvaluation(EvaluationDto.StartEvaluationReq dto)을
        // 넘겨받은 상세 데이터에 맞게 가공하여 오버로딩 혹은 수정해서 호출하시면 됩니다.
        evaluationService.startEvaluation(response);
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
