package com.example.adcheck.service;

import com.example.adcheck.model.AdCheckDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiJudgeService {

    private static final Logger log = LoggerFactory.getLogger(AiJudgeService.class);
    private static final Pattern JSON_FENCE_PATTERN = Pattern.compile("(?s)```(?:json)?\\s*(\\{.*?})\\s*```");

    private final RestClient aiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${ai-judge.n8n.webhook-url}${ai-judge.n8n.check-endpoint}")
    private String adCheckUrl;

    public AiJudgeService(
            @Qualifier("aiRestClient") RestClient aiRestClient,
            ObjectMapper objectMapper
    ) {
        this.aiRestClient = aiRestClient;
        this.objectMapper = objectMapper;
    }

    public AdCheckDto.Res check(String copy) {
        AdCheckDto.Req req = new AdCheckDto.Req(copy);

        try {
            log.info("Calling n8n ad check. url={}, copyLength={}", adCheckUrl, copy == null ? 0 : copy.length());
            byte[] rawBytes = aiRestClient.post()
                    .uri(adCheckUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_OCTET_STREAM)
                    .body(req)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.NOT_FOUND, (request, response) -> {
                        throw new RuntimeException("n8n ad check endpoint was not found.");
                    })
                    .onStatus(status -> status.is5xxServerError(), (request, response) -> {
                        throw new RuntimeException("n8n server failed while processing ad check.");
                    })
                    .body(byte[].class);

            String raw = rawBytes == null ? "" : new String(rawBytes, StandardCharsets.UTF_8);
            log.info("n8n ad check response received. responseLength={}", raw.length());
            return parseResponse(raw);
        } catch (ResourceAccessException e) {
            log.error("n8n ad check connection/read failed. url={}", adCheckUrl, e);
            throw new RuntimeException("n8n response timed out or the server is unreachable.", e);
        } catch (RestClientResponseException e) {
            log.error("n8n ad check returned error status. url={}, status={}, body={}",
                    adCheckUrl, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException(
                    "n8n call failed. HTTP " + e.getStatusCode() + " response: " + e.getResponseBodyAsString(),
                    e
            );
        } catch (RestClientException e) {
            log.error("n8n ad check RestClient failed. url={}", adCheckUrl, e);
            throw new RuntimeException("AI judge server connection failed.", e);
        }
    }

    private AdCheckDto.Res parseResponse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw.trim());
            JsonNode result = root;

            if (root.hasNonNull("output")) {
                String output = root.get("output").asText();
                result = objectMapper.readTree(extractJsonPayload(output));
            }

            return toAdCheckResponse(result);
        } catch (Exception e) {
            throw new RuntimeException("AI judge response cannot be parsed: " + raw, e);
        }
    }

    private String extractJsonPayload(String content) {
        String trimmed = content == null ? "" : content.trim();
        Matcher matcher = JSON_FENCE_PATTERN.matcher(trimmed);
        String fencedJson = null;
        while (matcher.find()) {
            fencedJson = matcher.group(1);
        }
        if (fencedJson != null) {
            return fencedJson.trim();
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1).trim();
        }
        return trimmed;
    }

    private AdCheckDto.Res toAdCheckResponse(JsonNode result) throws IOException {
        if (result.has("status")) {
            return objectMapper.treeToValue(result, AdCheckDto.Res.class);
        }

        String status = text(result, "final_status", "status");
        JsonNode review = selectReview(result, status);

        String law = text(review, "law");
        String violationText = text(review, "violation_text", "violationText");
        String reason = text(review, "reason");
        String suggestion = text(review, "suggestion");

        if (!hasText(reason)) {
            reason = text(result, "summary");
        }

        return new AdCheckDto.Res(
                hasText(status) ? status : "pass",
                law,
                violationText,
                reason,
                suggestion
        );
    }

    private JsonNode selectReview(JsonNode result, String finalStatus) {
        JsonNode legalReview = result.path("legal_review");
        JsonNode brandReview = result.path("brand_review");

        if (sameStatus(legalReview, finalStatus)) {
            return legalReview;
        }
        if (sameStatus(brandReview, finalStatus)) {
            return brandReview;
        }
        if (!legalReview.isMissingNode()) {
            return legalReview;
        }
        if (!brandReview.isMissingNode()) {
            return brandReview;
        }
        return result;
    }

    private boolean sameStatus(JsonNode node, String status) {
        return hasText(status) && status.equalsIgnoreCase(text(node, "status"));
    }

    private String text(JsonNode node, String... names) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        for (String name : names) {
            JsonNode value = node.path(name);
            if (value.isTextual() && hasText(value.asText())) {
                return value.asText().trim();
            }
        }
        return "";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
