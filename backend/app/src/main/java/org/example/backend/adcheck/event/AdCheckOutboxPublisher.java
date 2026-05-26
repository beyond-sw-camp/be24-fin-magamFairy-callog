package org.example.backend.adcheck.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.adcheck.model.AdCheckOutboxEvent;
import org.example.backend.adcheck.repository.AdCheckOutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdCheckOutboxPublisher {
    private final AdCheckOutboxEventRepository adCheckOutboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;

    @Value("${ad-check.outbox.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Value("${ad-check.outbox.kafka.topic:ad-check.result-events}")
    private String topic;

    @Scheduled(fixedDelayString = "${ad-check.outbox.publish-delay-ms:5000}")
    public void publishPendingEvents() {
        if (!kafkaEnabled) {
            return;
        }

        List<AdCheckOutboxEvent> events = transactionTemplate.execute(status ->
                adCheckOutboxEventRepository.findTop50ByStatusInOrderByCreatedAtAsc(List.of(
                        AdCheckOutboxEvent.STATUS_PENDING,
                        AdCheckOutboxEvent.STATUS_FAILED
                ))
        );
        if (events == null || events.isEmpty()) {
            return;
        }

        for (AdCheckOutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(AdCheckOutboxEvent event) {
        try {
            kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload()).get(5, TimeUnit.SECONDS);
            transactionTemplate.executeWithoutResult(status -> adCheckOutboxEventRepository
                    .findById(event.getIdx())
                    .ifPresent(AdCheckOutboxEvent::markPublished));
        } catch (Exception e) {
            log.warn("Ad check outbox publish failed. eventId={}, eventType={}",
                    event.getIdx(), event.getEventType(), e);
            transactionTemplate.executeWithoutResult(status -> adCheckOutboxEventRepository
                    .findById(event.getIdx())
                    .ifPresent(current -> current.markFailed(e.getMessage())));
        }
    }
}
