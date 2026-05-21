package org.example.evaluation.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EvaluationKafkaConsumer {

    @KafkaListener(
            topics = "${app.kafka.topics.evaluation-start}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeStartEvaluation(String message) {
        log.info("[Kafka] evaluation.start received: {}", message);
    }

    @KafkaListener(
            topics = "${app.kafka.topics.evaluation-collect}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeCollectEvaluation(String message) {
        log.info("[Kafka] evaluation.collect received: {}", message);
    }
}