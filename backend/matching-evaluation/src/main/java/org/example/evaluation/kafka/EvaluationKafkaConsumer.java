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
     * String으로 안전하게 받아와 ObjectMapper를 통해 객체로 변환합니다.
     */
    @KafkaListener(
            topics = "${app.kafka.topics.evaluation-start-reply:evaluation.start-reply}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeMainModuleResponse(String message) { // 💡 EvaluationDto.StartEvaluation에서 String으로 변경
        log.info("[Evaluation MSA] 메인 모듈로부터 응답 수신 (Raw Payload): {}", message);

        try {
            // 💡 명시적으로 Json 문자열을 수신 측 DTO 객체로 변환
            EvaluationDto.StartEvaluation response =
                    objectMapper.readValue(message, EvaluationDto.StartEvaluation.class);

            log.info("[Evaluation MSA] 메인 모듈로부터 데이터 스냅샷 수신 및 파싱 완료. campaignIdx(publicId)={}",
                    response.getCampaignIdx());

            // 수신한 데이터를 가지고 n8n 웹훅을 호출하는 비즈니스 로직 가동
            evaluationService.startEvaluation(response);

        } catch (JsonProcessingException e) {
            log.error("[Evaluation MSA] 응답 메시지 역직렬화(JSON 파싱) 실패. message={}", message, e);
            // 필요 시 에러 핸들링 파이프라인 전송 가능 (예: sendStartDeadLetter)
        } catch (RuntimeException e) {
            log.error("[Evaluation MSA] 응답 처리 중 비즈니스 로직 에러 발생", e);
        }
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