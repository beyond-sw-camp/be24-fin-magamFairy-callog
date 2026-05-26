package org.example.backend.adcheck.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.adcheck.model.AdCheckDto;
import org.example.backend.adcheck.model.AdCheckJob;
import org.example.backend.adcheck.repository.AdCheckJobRepository;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdCheckJobProjectionService {
    private static final String EVENT_COMPLETED = "AI_JUDGE_COMPLETED";
    private static final String EVENT_FAILED = "AI_JUDGE_FAILED";

    private final AdCheckJobRepository adCheckJobRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void upsertDirectAiJudgeJob(JsonNode event) {
        String eventType = text(event, "eventType");
        if (!EVENT_COMPLETED.equals(eventType) && !EVENT_FAILED.equals(eventType)) {
            return;
        }

        JsonNode context = event == null ? null : event.path("context");
        if (hasText(context, "adCheckJobId")) {
            return;
        }

        String analysisJobId = text(event, "analysisJobId");
        String campaignPublicId = text(context, "campaignId");
        Long requesterIdx = longValue(context == null ? null : context.path("requesterUserIdx"));
        if (analysisJobId == null || campaignPublicId == null || requesterIdx == null) {
            log.warn(
                    "Skipped direct ai-judge job projection because context is missing. analysisJobId={}, campaignId={}, requesterIdx={}",
                    analysisJobId,
                    campaignPublicId,
                    requesterIdx
            );
            return;
        }

        Campaign campaign = campaignRepository.findByPublicId(campaignPublicId).orElse(null);
        User requester = userRepository.findWithOrganizationByIdx(requesterIdx)
                .or(() -> userRepository.findByIdx(requesterIdx))
                .orElse(null);
        if (campaign == null || requester == null) {
            log.warn(
                    "Skipped direct ai-judge job projection because campaign or requester is missing. analysisJobId={}, campaignId={}, requesterIdx={}",
                    analysisJobId,
                    campaignPublicId,
                    requesterIdx
            );
            return;
        }

        if (campaignMemberRepository.findByCampaignIdxAndUserIdx(campaign.getIdx(), requesterIdx).isEmpty()) {
            log.warn(
                    "Skipped direct ai-judge job projection because requester is not a campaign member. analysisJobId={}, campaignId={}, requesterIdx={}",
                    analysisJobId,
                    campaignPublicId,
                    requesterIdx
            );
            return;
        }

        AdCheckDto.FileCheckRes result = toFileCheckResponse(event);
        String resultPayload = serializeResult(result);
        AdCheckJob job = adCheckJobRepository.findByJobId(analysisJobId)
                .orElseGet(() -> AdCheckJob.queued(
                        analysisJobId,
                        analysisJobId,
                        requester,
                        campaignPublicId,
                        firstText(result.getFileName(), "upload"),
                        result.getFileContentType(),
                        result.getFileSize(),
                        null
                ));

        job.markRunning();
        if (EVENT_FAILED.equals(eventType)) {
            String errorMessage = firstText(result.getErrorMessage(), "AI judge processing failed.");
            job.markFailed(
                    errorMessage,
                    resultPayload,
                    analysisJobId,
                    normalizeResultStatus(result.getStatus()),
                    resolveRiskLevel(result, errorMessage),
                    resolveSummaryMessage(result, errorMessage)
            );
        } else {
            job.markSucceeded(
                    resultPayload,
                    analysisJobId,
                    normalizeResultStatus(result.getStatus()),
                    resolveRiskLevel(result, null),
                    resolveSummaryMessage(result, null)
            );
        }

        adCheckJobRepository.save(job);
        log.info(
                "Projected direct ai-judge event to ad_check_job. jobId={}, campaignId={}, requesterIdx={}, eventType={}",
                analysisJobId,
                campaignPublicId,
                requesterIdx,
                eventType
        );
    }

    private AdCheckDto.FileCheckRes toFileCheckResponse(JsonNode event) {
        String eventType = text(event, "eventType");
        String errorMessage = text(event, "errorMessage");
        if (EVENT_FAILED.equals(eventType) && errorMessage == null) {
            errorMessage = "AI judge processing failed.";
        }

        return AdCheckDto.FileCheckRes.builder()
                .analysisJobId(text(event, "analysisJobId"))
                .analysisObjectPrefix(text(event, "analysisObjectPrefix"))
                .fileName(text(event, "fileName"))
                .fileObjectKey(text(event, "fileObjectKey"))
                .fileContentType(text(event, "fileContentType"))
                .fileSize(longValue(event == null ? null : event.path("fileSize")))
                .status(firstText(text(event, "aiStatus"), EVENT_FAILED.equals(eventType) ? "failed" : null))
                .law(text(event, "law"))
                .violationText(text(event, "violationText"))
                .reason(text(event, "reason"))
                .suggestion(text(event, "suggestion"))
                .verdictLevel(integer(event, "verdictLevel", "verdict_level", "reviewLevel", "review_level", "riskLevel", "risk_level", "level", "grade"))
                .extractionMode(text(event, "extractionMode"))
                .finalResultObjectKey(text(event, "finalResultObjectKey"))
                .errorMessage(errorMessage)
                .context(contextMap(event == null ? null : event.path("context")))
                .build();
    }

    private Map<String, Object> contextMap(JsonNode context) {
        if (context == null || context.isMissingNode() || context.isNull() || !context.isObject()) {
            return Map.of();
        }
        return objectMapper.convertValue(context, new TypeReference<>() {});
    }

    private String serializeResult(AdCheckDto.FileCheckRes result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("Direct ai-judge result serialization failed. analysisJobId={}",
                    result == null ? null : result.getAnalysisJobId(), e);
            return "{}";
        }
    }

    private String normalizeResultStatus(String status) {
        String normalized = firstText(status);
        return normalized == null ? null : normalized.toLowerCase();
    }

    private String resolveRiskLevel(AdCheckDto.FileCheckRes result, String errorMessage) {
        if (errorMessage != null && !errorMessage.isBlank()) {
            return "HIGH";
        }

        Integer verdictLevel = result == null ? null : result.getVerdictLevel();
        if (verdictLevel != null && verdictLevel >= 1 && verdictLevel <= 5) {
            return String.valueOf(verdictLevel);
        }

        String status = normalizeResultStatus(result == null ? null : result.getStatus());
        if ("pass".equals(status)) {
            return "LOW";
        }
        if ("warning".equals(status)) {
            return "MEDIUM";
        }
        if ("violation".equals(status) || "failed".equals(status)) {
            return "HIGH";
        }
        return "NORMAL";
    }

    private String resolveSummaryMessage(AdCheckDto.FileCheckRes result, String errorMessage) {
        if (errorMessage != null && !errorMessage.isBlank()) {
            return errorMessage.trim();
        }

        String status = normalizeResultStatus(result == null ? null : result.getStatus());
        if ("pass".equals(status)) {
            return "AI check completed.";
        }
        if ("warning".equals(status) || "violation".equals(status)) {
            return firstText(
                    result == null ? null : result.getReason(),
                    result == null ? null : result.getViolationText(),
                    "AI check result needs review."
            );
        }
        return "AI check result is available.";
    }

    private String text(JsonNode node, String name) {
        if (node == null || name == null || node.path(name).isMissingNode() || node.path(name).isNull()) {
            return null;
        }
        String value = node.path(name).asText(null);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Long longValue(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.canConvertToLong()) {
            return value.asLong();
        }
        try {
            String raw = value.asText(null);
            return raw == null || raw.isBlank() ? null : Long.parseLong(raw.trim());
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

    private boolean hasText(JsonNode node, String name) {
        return text(node, name) != null;
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
