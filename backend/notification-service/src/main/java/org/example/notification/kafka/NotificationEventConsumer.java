package org.example.notification.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notification.event.NotificationEvent;
import org.example.notification.service.NotificationCommandService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// Kafka에서 알림 이벤트를 받아 알림 저장과 SSE 발행 흐름으로 연결하는 Consumer입니다.
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {
    private final NotificationCommandService commandService;
    private final NotificationDeadLetterProducer deadLetterProducer;

    // notification.events 토픽에서 이벤트를 받아 알림 생성 명령으로 넘깁니다.
    @KafkaListener(
            topics = "${notification.kafka.topic:notification.events}",
            groupId = "${notification.kafka.group-id:notification-service}",
            containerFactory = "notificationEventKafkaListenerContainerFactory"
    )
    public void consume(NotificationEvent event) {
        try {
            commandService.createFromEvent(event);
        } catch (Exception exception) {
            log.warn("Notification event processing failed. eventId={}", event == null ? null : event.eventId(), exception);
            deadLetterProducer.send(event, exception);
        }
    }
}
