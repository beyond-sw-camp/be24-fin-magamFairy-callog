package org.example.backend.adcheck.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.adcheck.model.AdCheckDto;
import org.example.backend.adcheck.model.AdCheckJobDto;
import org.example.backend.adcheck.service.AdAiAnalysisService;
import org.example.backend.adcheck.service.AdCheckJobService;
import org.example.backend.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AiJudgeKafkaEventListener {
    private final ObjectMapper objectMapper;
    private final AdAiAnalysisService adAiAnalysisService;
    private final AdCheckJobService adCheckJobService;
    private final NotificationService notificationService;

    public AiJudgeKafkaEventListener(
            ObjectMapper objectMapper,
            AdAiAnalysisService adAiAnalysisService,
            AdCheckJobService adCheckJobService,
            NotificationService notificationService
    ) {
        this.objectMapper = objectMapper;
        this.adAiAnalysisService = adAiAnalysisService;
        this.adCheckJobService = adCheckJobService;
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "${ai-judge.kafka.completed-topic:ai-judge.completed}",
            groupId = "${ai-judge.kafka.group-id:callog-app-ai-judge}",
            autoStartup = "${ai-judge.kafka.enabled:false}"
    )
    public void handleAiJudgeCompleted(String payload) {
        try {
            JsonNode event = objectMapper.readTree(payload);
            log.info(
                    "Consumed ai-judge Kafka event. eventType={}, analysisJobId={}, aiStatus={}, fileName={}",
                    text(event, "eventType"),
                    text(event, "analysisJobId"),
                    text(event, "aiStatus"),
                    text(event, "fileName")
            );
            String eventType = text(event, "eventType");
            String appManagedJobId = text(event.path("context"), "adCheckJobId");

            if ("AI_JUDGE_PROGRESS".equals(eventType)) {
                applyAppManagedProgress(event, appManagedJobId);
                return;
            }

            if (hasText(appManagedJobId)) {
                applyAppManagedResult(event, eventType, appManagedJobId);
                applyAnalysisProjection(event);
                log.info(
                        "Skip ai-judge Kafka notification for app-managed ad check job. jobId={}, analysisJobId={}",
                        appManagedJobId,
                        text(event, "analysisJobId")
                );
                return;
            }

            adAiAnalysisService.applyAiJudgeEvent(event);

            Long requesterIdx = longValue(event.path("context").path("requesterUserIdx"));
            if (requesterIdx == null) {
                log.warn(
                        "AI judge Kafka event has no requester context. eventType={}, analysisJobId={}",
                        text(event, "eventType"),
                        text(event, "analysisJobId")
                );
                return;
            }

            notificationService.notifyAiJudgeResult(requesterIdx, toFileCheckResponse(event));
        } catch (Exception e) {
            log.warn("AI judge Kafka event cannot be parsed. payload={}", payload, e);
        }
    }

    private void applyAnalysisProjection(JsonNode event) {
        try {
            adAiAnalysisService.applyAiJudgeEvent(event);
        } catch (Exception e) {
            log.warn("AI judge campaign analysis projection update failed. analysisJobId={}",
                    text(event, "analysisJobId"), e);
        }
    }

    private void applyAppManagedProgress(JsonNode event, String jobId) {
        if (!hasText(jobId)) {
            return;
        }
        String token = text(event.path("context"), "adCheckProgressToken");
        String step = text(event, "step");
        if (!hasText(token) || !hasText(step)) {
            log.warn("Skipped AI judge progress event because token or step is missing. jobId={}", jobId);
            return;
        }
        adCheckJobService.updateProgress(new AdCheckJobDto.ProgressReq(jobId, token, step));
    }

    private void applyAppManagedResult(JsonNode event, String eventType, String jobId) {
        AdCheckDto.FileCheckRes response = toFileCheckResponse(event);
        if ("AI_JUDGE_FAILED".equals(eventType)) {
            adCheckJobService.failFromAiJudgeEvent(jobId, response, text(event, "errorMessage"));
            return;
        }
        adCheckJobService.completeFromAiJudgeEvent(jobId, response);
    }

    private AdCheckDto.FileCheckRes toFileCheckResponse(JsonNode event) {
        JsonNode result = event == null ? null : event.path("result");
        if (result != null && result.isObject()) {
            try {
                return objectMapper.treeToValue(result, AdCheckDto.FileCheckRes.class);
            } catch (Exception e) {
                log.warn("AI judge result payload cannot be converted. analysisJobId={}",
                        text(event, "analysisJobId"), e);
            }
        }

        String eventType = text(event, "eventType");
        String errorMessage = text(event, "errorMessage");
        if ("AI_JUDGE_FAILED".equals(eventType) && errorMessage.isBlank()) {
            errorMessage = "AI judge processing failed.";
        }

        return AdCheckDto.FileCheckRes.builder()
                .analysisJobId(text(event, "analysisJobId"))
                .analysisObjectPrefix(text(event, "analysisObjectPrefix"))
                .fileName(text(event, "fileName"))
                .fileObjectKey(text(event, "fileObjectKey"))
                .fileContentType(text(event, "fileContentType"))
                .fileSize(longValue(event.path("fileSize")))
                .status(text(event, "aiStatus"))
                .law(text(event, "law"))
                .violationText(text(event, "violationText"))
                .reason(text(event, "reason"))
                .suggestion(text(event, "suggestion"))
                .verdictLevel(integer(event, "verdictLevel", "verdict_level", "reviewLevel", "review_level", "riskLevel", "risk_level", "level", "grade"))
                .extractionMode(text(event, "extractionMode"))
                .finalResultObjectKey(text(event, "finalResultObjectKey"))
                .errorMessage(errorMessage)
                .build();
    }

    private String text(JsonNode node, String name) {
        JsonNode value = node == null ? null : node.path(name);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return "";
        }
        return value.asText("");
    }

    private Long longValue(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.canConvertToLong()) {
            return value.asLong();
        }
        try {
            String raw = value.asText("");
            return raw.isBlank() ? null : Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer integer(JsonNode node, String... names) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String name : names) {
            Integer parsed = intValue(node.path(name));
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private Integer intValue(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.canConvertToInt()) {
            int number = value.asInt();
            return number >= 1 && number <= 5 ? number : null;
        }
        String raw = value.asText("").trim();
        for (int index = 0; index < raw.length(); index++) {
            char current = raw.charAt(index);
            if (current >= '1' && current <= '5') {
                return current - '0';
            }
        }
        return null;
    }

    private boolean hasText(JsonNode value) {
        return value != null && !value.isMissingNode() && !value.isNull() && !value.asText("").isBlank();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
