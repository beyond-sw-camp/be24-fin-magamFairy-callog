package org.example.backend.adcheck.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.adcheck.model.AdCheckDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AdCheckService {

    private final RestClient aiRestClient;
    private static final Pattern JSON_FENCE_PATTERN = Pattern.compile("(?s)```(?:json)?\\s*(\\{.*?})\\s*```");

    private final ObjectMapper objectMapper;
    private final TextExtractorService textExtractorService;
    private final AdCheckFileStorageService adCheckFileStorageService;

    @Value("${custom.n8n.webhook-url}/check")
    private String adCheckUrl;

    public AdCheckService(
            @Qualifier("aiRestClient") RestClient aiRestClient,
            ObjectMapper objectMapper,
            TextExtractorService textExtractorService,
            AdCheckFileStorageService adCheckFileStorageService
    ) {
        this.aiRestClient = aiRestClient;
        this.objectMapper = objectMapper;
        this.textExtractorService = textExtractorService;
        this.adCheckFileStorageService = adCheckFileStorageService;
    }

    public AdCheckDto.Res check(String copy) {
        AdCheckDto.Req req = AdCheckDto.Req.builder().copy(copy).build();

        try {
            log.info("Calling n8n ad check. url={}, copyLength={}", adCheckUrl, copy == null ? 0 : copy.length());
            byte[] rawBytes = aiRestClient.post()
                    .uri(adCheckUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_OCTET_STREAM)
                    .body(req)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.NOT_FOUND, (request, response) -> {
                        throw new RuntimeException("n8n 엔드포인트를 찾을 수 없습니다.");
                    })
                    .onStatus(status -> status.is5xxServerError(), (request, response) -> {
                        throw new RuntimeException("n8n 서버 처리 중 오류가 발생했습니다.");
                    })
                    .body(byte[].class);

            String raw = rawBytes == null ? "" : new String(rawBytes, StandardCharsets.UTF_8);
            log.info("n8n ad check response received. responseLength={}", raw == null ? 0 : raw.length());
            return parseResponse(raw);

        } catch (ResourceAccessException e) {
            log.error("n8n ad check connection/read failed. url={}", adCheckUrl, e);
            throw new RuntimeException("n8n 응답 대기 시간이 초과되었거나 서버에 연결할 수 없습니다. n8n 워크플로우가 활성화되어 있고 AI 처리 후 Respond to Webhook까지 도달하는지 확인해주세요.", e);
        } catch (RestClientResponseException e) {
            log.error("n8n ad check returned error status. url={}, status={}, body={}", adCheckUrl, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("n8n 호출이 실패했습니다. HTTP " + e.getStatusCode() + " 응답: " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            log.error("n8n ad check RestClient failed. url={}", adCheckUrl, e);
            throw new RuntimeException("AI 검수 서버와 연결할 수 없습니다.", e);
        }
    }

    public AdCheckDto.FileCheckRes checkFile(MultipartFile file) {
        try {
            AdCheckFileStorageService.StoredFile storedFile = adCheckFileStorageService.upload(file);
            String text = textExtractorService.extract(file);
            AdCheckDto.Res result = check(text);
            return AdCheckDto.FileCheckRes.of(
                    file.getOriginalFilename(),
                    storedFile.getObjectKey(),
                    storedFile.getViewUrl(),
                    storedFile.getContentType(),
                    storedFile.getFileSize(),
                    text,
                    result
            );
        } catch (IOException e) {
            throw new RuntimeException("파일 처리 중 오류가 발생했습니다.", e);
        }
    }

    // n8n AI Agent는 {"output": "...json string..."} 형태로 응답함
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
            throw new RuntimeException("AI 검수 결과를 파싱할 수 없습니다: " + raw, e);
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
