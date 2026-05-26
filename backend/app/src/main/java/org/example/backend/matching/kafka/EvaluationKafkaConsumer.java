package org.example.backend.matching.kafka; // 모놀리식 패키지 경로

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.matching.event.EvaluationStartRequestedEvent;
import org.example.backend.matching.model.evaluation.EvaluationDto;
import org.example.backend.matching.service.EvaluationService; // 모놀리식 서비스
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final EvaluationService evaluationService;

    /**
     * 📥 평가 모듈이 보낸 질문(식별자 엽서)을 낚아채는 리스너
     */
    @KafkaListener(
            topics = "${app.kafka.topics.evaluation-start:evaluation.start}",
            groupId = "main-monolithic-group" // 모놀리식 전용 그룹 ID
    )
    public void consumeEvaluationQuery(String message) {
        log.info("[Monolithic] 평가 모듈로부터 요청 접수 (Raw Payload): {}", message);
        try {
            // String으로 받아서 역직렬화하는 것이 패키지 불일치 에러를 피하기 가장 안전합니다.
            EvaluationStartRequestedEvent queryEvent =
                    objectMapper.readValue(message, EvaluationStartRequestedEvent.class);
            // Step 3에서 만들 비즈니스 로직 메서드 호출
            evaluationService.startEvaluation(EvaluationDto.StartEvaluationReq.from(queryEvent));

        } catch (Exception e) {
            log.error("[Monolithic] 요청 처리 중 에러 발생: ", e);
        }
    }
}