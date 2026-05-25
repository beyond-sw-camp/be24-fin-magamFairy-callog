package org.example.notification.kafka;

import lombok.extern.slf4j.Slf4j;
import org.example.notification.event.NotificationEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// 처리 실패한 알림 이벤트를 실패 전용 Kafka 토픽으로 보내는 Producer입니다.
@Slf4j
@Component
public class NotificationDeadLetterProducer {
    private final KafkaTemplate<String, NotificationDeadLetterEvent> kafkaTemplate;
    private final String deadLetterTopic;

    public NotificationDeadLetterProducer(
            KafkaTemplate<String, NotificationDeadLetterEvent> kafkaTemplate,
            @Value("${notification.kafka.dlt-topic:notification.events.dlt}") String deadLetterTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.deadLetterTopic = deadLetterTopic;
    }

    // 실패한 원본 이벤트와 실패 사유를 DLT 토픽으로 발행합니다.
    public void send(NotificationEvent event, Exception exception) {
        NotificationDeadLetterEvent deadLetterEvent = NotificationDeadLetterEvent.from(event, exception);
        kafkaTemplate.send(deadLetterTopic, resolveKey(event), deadLetterEvent);
        log.warn("Notification event sent to DLT. eventId={}, reason={}", resolveKey(event), deadLetterEvent.reason());
    }

    private String resolveKey(NotificationEvent event) {
        return event == null || event.eventId() == null || event.eventId().isBlank()
                ? "unknown"
                : event.eventId();
    }
}
