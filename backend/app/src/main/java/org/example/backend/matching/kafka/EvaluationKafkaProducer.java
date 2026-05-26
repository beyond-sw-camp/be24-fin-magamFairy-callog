package org.example.backend.matching.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.matching.model.evaluation.EvaluationDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.evaluation-start}")
    private String evaluationStartTopic;

    @Value("${app.kafka.topics.evaluation-collect}")
    private String evaluationCollectTopic;

    // 💡 수정: 모듈의 요청에 '답변'을 줄 토픽 경로로 바꿉니다.
    @Value("${app.kafka.topics.evaluation-start-reply:evaluation.start-reply}")
    private String evaluationStartReplyTopic;

    public void sendStartReply(EvaluationDto.StartEvaluation request) {
        // 답변을 보낼 때는 campaignIdx(publicId)를 메시지 키로 사용
        log.info("[Evaluation MSA] start request sending. benefitIdx={}, campaignIdx={}",
                request.getBenefit().getIdx(), request.getCampaign().idx());
        send(evaluationStartReplyTopic, request.getCampaignIdx(), request);
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