package org.example.backend.adcheck.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiJudgeKafkaRequestPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai-judge.kafka.enabled:false}")
    private boolean enabled;

    @Value("${ai-judge.kafka.request-topic:ai-judge.requests}")
    private String requestTopic;

    @Value("${ai-judge.kafka.send-timeout-ms:10000}")
    private long sendTimeoutMillis;

    public void publishFileCheckRequest(
            String jobId,
            String fileName,
            String contentType,
            Long fileSize,
            String fileObjectKey,
            byte[] fallbackFileBytes,
            Map<String, Object> context
    ) {
        if (!enabled) {
            throw new IllegalStateException("AI judge Kafka is disabled.");
        }
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("AI judge jobId is required.");
        }
        if ((fileObjectKey == null || fileObjectKey.isBlank())
                && (fallbackFileBytes == null || fallbackFileBytes.length == 0)) {
            throw new IllegalArgumentException("AI judge file payload is empty.");
        }

        try {
            String payload = objectMapper.writeValueAsString(toEvent(
                    jobId,
                    fileName,
                    contentType,
                    fileSize,
                    fileObjectKey,
                    fallbackFileBytes,
                    context
            ));
            kafkaTemplate.send(requestTopic, jobId, payload).get(sendTimeoutMillis, TimeUnit.MILLISECONDS);
            log.info("Published AI judge file check request. topic={}, jobId={}, fileName={}, size={}",
                    requestTopic, jobId, fileName, fileSize);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI judge Kafka request publish interrupted.", e);
        } catch (Exception e) {
            throw new IllegalStateException("AI judge Kafka request publish failed.", e);
        }
    }

    private Map<String, Object> toEvent(
            String jobId,
            String fileName,
            String contentType,
            Long fileSize,
            String fileObjectKey,
            byte[] fallbackFileBytes,
            Map<String, Object> context
    ) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "AI_JUDGE_FILE_CHECK_REQUEST");
        event.put("occurredAt", Instant.now().toString());
        event.put("jobId", jobId);
        event.put("analysisJobId", jobId);
        event.put("fileName", fileName);
        event.put("fileObjectKey", fileObjectKey);
        event.put("fileContentType", contentType);
        event.put("fileSize", fileSize);
        if (fileObjectKey == null || fileObjectKey.isBlank()) {
            event.put("fileBase64", Base64.getEncoder().encodeToString(fallbackFileBytes));
        }
        event.put("context", context == null ? Map.of() : Map.copyOf(context));
        return event;
    }
}
