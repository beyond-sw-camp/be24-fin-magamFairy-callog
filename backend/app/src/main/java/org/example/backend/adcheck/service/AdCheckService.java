package org.example.backend.adcheck.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.adcheck.analysis.service.AdCheckAnalysisMongoStorageService;
import org.example.backend.adcheck.client.AiJudgeClient;
import org.example.backend.adcheck.model.AdCheckDto;
import org.example.backend.notification.service.NotificationService;
import org.example.backend.user.model.AuthUserDetails;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AdCheckService {

    private final RestClient aiRestClient;
    private final AiJudgeClient aiJudgeClient;
    private static final Pattern JSON_FENCE_PATTERN = Pattern.compile("(?s)```(?:json)?\\s*(\\{.*?})\\s*```");
    private static final String EXTRACTED_TEXT_PATH = "ocr/extracted-text.txt";
    private static final String AI_RESULT_PATH = "ai/result.json";
    private static final String AI_ERROR_PATH = "ai/error.json";
    private static final String FINAL_RESULT_PATH = "final/result.json";

    private final ObjectMapper objectMapper;
    private final TextExtractorService textExtractorService;
    private final AdCheckFileStorageService adCheckFileStorageService;
    private final AdCheckAnalysisMongoStorageService adCheckAnalysisMongoStorageService;
    private final NotificationService notificationService;

    @Value("${custom.n8n.webhook-url}${custom.n8n.check-endpoint}")
    private String adCheckUrl;

    public AdCheckService(
            @Qualifier("aiRestClient") RestClient aiRestClient,
            AiJudgeClient aiJudgeClient,
            ObjectMapper objectMapper,
            TextExtractorService textExtractorService,
            AdCheckFileStorageService adCheckFileStorageService,
            AdCheckAnalysisMongoStorageService adCheckAnalysisMongoStorageService,
            NotificationService notificationService
    ) {
        this.aiRestClient = aiRestClient;
        this.aiJudgeClient = aiJudgeClient;
        this.objectMapper = objectMapper;
        this.textExtractorService = textExtractorService;
        this.adCheckFileStorageService = adCheckFileStorageService;
        this.adCheckAnalysisMongoStorageService = adCheckAnalysisMongoStorageService;
        this.notificationService = notificationService;
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
            throw new RuntimeException(
                    "n8n 응답 대기 시간이 초과되었거나 서버에 연결할 수 없습니다. "
                            + "n8n 워크플로우가 활성화되어 있고 AI 처리 후 Respond to Webhook까지 도달하는지 확인해주세요.",
                    e
            );
        } catch (RestClientResponseException e) {
            log.error("n8n ad check returned error status. url={}, status={}, body={}",
                    adCheckUrl, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException(
                    "n8n 호출에 실패했습니다. HTTP " + e.getStatusCode() + " 응답: " + e.getResponseBodyAsString(),
                    e
            );
        } catch (RestClientException e) {
            log.error("n8n ad check RestClient failed. url={}", adCheckUrl, e);
            throw new RuntimeException("AI 검수 서버와 연결할 수 없습니다.", e);
        }
    }

    public AdCheckDto.Res checkWithAiJudge(String copy) {
        return aiJudgeClient.check(copy);
    }

    public AdCheckDto.FileCheckRes checkFile(MultipartFile file) {
        long totalStartedAt = System.nanoTime();
        try {
            AdCheckFileStorageService.AnalysisStorageContext storageContext =
                    adCheckFileStorageService.createAnalysisStorageContext(file == null ? null : file.getOriginalFilename());
            AdCheckFileStorageService.StoredFile storedFile =
                    adCheckFileStorageService.uploadOriginal(file, storageContext);
            TextExtractorService.ExtractResult extraction = textExtractorService.extractWithTiming(file);
            AdCheckFileStorageService.StoredFile extractedTextFile =
                    adCheckFileStorageService.uploadText(storageContext, EXTRACTED_TEXT_PATH, extraction.text());
            List<AdCheckDto.FileArtifact> imageArtifacts =
                    uploadExtractedImageAssets(storageContext, extraction.extractedImages());
            long aiStartedAt = System.nanoTime();

            try {
                AdCheckDto.Res result = check(extraction.text());
                long aiAnalysisMillis = elapsedMillis(aiStartedAt);
                AdCheckFileStorageService.StoredFile aiResultFile =
                        uploadJsonArtifact(storageContext, AI_RESULT_PATH, result);
                AdCheckDto.FileCheckRes response = buildFileCheckResponse(
                        storageContext,
                        file == null ? null : file.getOriginalFilename(),
                        storedFile.getObjectKey(),
                        storedFile.getViewUrl(),
                        storedFile.getContentType(),
                        storedFile.getFileSize(),
                        extractedTextFile,
                        imageArtifacts,
                        aiResultFile,
                        result,
                        extraction.text(),
                        extraction.extractionMode(),
                        buildProcessingTimes(extraction, aiAnalysisMillis, totalStartedAt),
                        null
                );
                response = storeFinalResult(storageContext, response);
                adCheckAnalysisMongoStorageService.save(response);
                return response;
            } catch (RuntimeException e) {
                long aiAnalysisMillis = elapsedMillis(aiStartedAt);
                AdCheckFileStorageService.StoredFile aiErrorFile =
                        uploadJsonArtifact(storageContext, AI_ERROR_PATH, Map.of("errorMessage", String.valueOf(e.getMessage())));
                AdCheckDto.FileCheckRes partialResponse = buildFileCheckResponse(
                        storageContext,
                        file == null ? null : file.getOriginalFilename(),
                        storedFile.getObjectKey(),
                        storedFile.getViewUrl(),
                        storedFile.getContentType(),
                        storedFile.getFileSize(),
                        extractedTextFile,
                        imageArtifacts,
                        aiErrorFile,
                        null,
                        extraction.text(),
                        extraction.extractionMode(),
                        buildProcessingTimes(extraction, aiAnalysisMillis, totalStartedAt),
                        e.getMessage()
                );
                partialResponse = storeFinalResult(storageContext, partialResponse);
                adCheckAnalysisMongoStorageService.save(partialResponse);
                throw new FileCheckException(e.getMessage(), partialResponse, e);
            }
        } catch (IOException e) {
            throw new RuntimeException("파일 처리 중 오류가 발생했습니다.", e);
        }
    }

    public AdCheckDto.FileCheckRes checkFileWithAiJudge(MultipartFile file) {
        return checkFileWithAiJudge(file, null, null);
    }

    public AdCheckDto.FileCheckRes checkFileWithAiJudge(
            MultipartFile file,
            AuthUserDetails requester,
            String campaignId
    ) {
        try {
            AdCheckDto.FileCheckRes response = aiJudgeClient.checkFile(file, aiJudgeContext(requester, campaignId));
            notifyAiJudgeResult(requester, response);
            return response;
        } catch (AiJudgeClient.FileCheckRemoteException e) {
            notifyAiJudgeResult(requester, e.getResponse());
            throw new FileCheckException(e.getMessage(), e.getResponse(), e);
        }
    }

    private void notifyAiJudgeResult(AuthUserDetails requester, AdCheckDto.FileCheckRes response) {
        if (requester == null || requester.getIdx() == null) {
            return;
        }

        notificationService.notifyAiJudgeResult(requester.getIdx(), response);
    }

    private Map<String, Object> aiJudgeContext(AuthUserDetails requester, String campaignId) {
        Map<String, Object> context = new HashMap<>();
        if (requester != null) {
            if (requester.getIdx() != null) {
                context.put("requesterUserIdx", requester.getIdx());
            }
            if (requester.getId() != null && !requester.getId().isBlank()) {
                context.put("requesterLoginId", requester.getId());
            }
            if (requester.getName() != null && !requester.getName().isBlank()) {
                context.put("requesterName", requester.getName());
            }
        }
        if (campaignId != null && !campaignId.isBlank()) {
            context.put("campaignId", campaignId.trim());
        }
        return context;
    }

    private AdCheckDto.ProcessingTimes buildProcessingTimes(
            TextExtractorService.ExtractResult extraction,
            long aiAnalysisMillis,
            long totalStartedAt
    ) {
        return AdCheckDto.ProcessingTimes.builder()
                .textExtractionMillis(extraction.textExtractionMillis())
                .layoutMillis(extraction.layoutMillis())
                .ocrMillis(extraction.ocrMillis())
                .aiAnalysisMillis(aiAnalysisMillis)
                .totalMillis(elapsedMillis(totalStartedAt))
                .build();
    }

    private List<AdCheckDto.FileArtifact> uploadExtractedImageAssets(
            AdCheckFileStorageService.AnalysisStorageContext storageContext,
            List<TextExtractorService.ExtractedImageAsset> extractedImages
    ) {
        if (extractedImages == null || extractedImages.isEmpty()) {
            return List.of();
        }

        Map<Integer, Integer> selectedByPage = new HashMap<>();
        List<AdCheckDto.FileArtifact> artifacts = new ArrayList<>();
        for (TextExtractorService.ExtractedImageAsset image : extractedImages) {
            int page = Math.max(0, image.page());
            int pageSequence = selectedByPage.getOrDefault(page, 0) + 1;
            selectedByPage.put(page, pageSequence);

            String relativePath = String.format(
                    Locale.ROOT,
                    "images/page-%03d/%02d_%s.png",
                    page,
                    pageSequence,
                    hasText(image.targetId()) ? image.targetId() : "image"
            );
            String contentType = hasText(image.contentType()) ? image.contentType() : MediaType.IMAGE_PNG_VALUE;
            AdCheckFileStorageService.StoredFile stored =
                    adCheckFileStorageService.uploadBytes(storageContext, relativePath, image.bytes(), contentType);

            artifacts.add(AdCheckDto.FileArtifact.builder()
                    .type("extracted_image")
                    .targetId(image.targetId())
                    .page(image.page())
                    .readingOrder(image.readingOrder())
                    .objectKey(stored.getObjectKey())
                    .url(stored.getViewUrl())
                    .contentType(stored.getContentType())
                    .fileSize(stored.getFileSize())
                    .build());
        }
        return artifacts;
    }

    private AdCheckDto.FileCheckRes buildFileCheckResponse(
            AdCheckFileStorageService.AnalysisStorageContext storageContext,
            String fileName,
            String fileObjectKey,
            String fileUrl,
            String fileContentType,
            Long fileSize,
            AdCheckFileStorageService.StoredFile extractedTextFile,
            List<AdCheckDto.FileArtifact> imageArtifacts,
            AdCheckFileStorageService.StoredFile aiResultFile,
            AdCheckDto.Res result,
            String extractedText,
            String extractionMode,
            AdCheckDto.ProcessingTimes processingTimes,
            String errorMessage
    ) {
        AdCheckDto.FileCheckRes.FileCheckResBuilder builder = AdCheckDto.FileCheckRes.builder()
                .analysisJobId(storageContext.getWorkId())
                .analysisObjectPrefix(storageContext.getBasePrefix())
                .fileName(fileName)
                .fileObjectKey(fileObjectKey)
                .fileUrl(fileUrl)
                .fileContentType(fileContentType)
                .fileSize(fileSize)
                .extractedTextObjectKey(extractedTextFile == null ? null : extractedTextFile.getObjectKey())
                .extractedTextUrl(extractedTextFile == null ? null : extractedTextFile.getViewUrl())
                .aiResultObjectKey(aiResultFile == null ? null : aiResultFile.getObjectKey())
                .aiResultUrl(aiResultFile == null ? null : aiResultFile.getViewUrl())
                .extractedImageAssets(imageArtifacts == null ? List.of() : List.copyOf(imageArtifacts))
                .extractedText(extractedText)
                .extractionMode(extractionMode)
                .processingTimes(processingTimes)
                .errorMessage(errorMessage);

        if (result != null) {
            builder.status(result.getStatus())
                    .law(result.getLaw())
                    .violationText(result.getViolationText())
                    .reason(result.getReason())
                    .suggestion(result.getSuggestion());
        }
        return builder.build();
    }

    private AdCheckDto.FileCheckRes storeFinalResult(
            AdCheckFileStorageService.AnalysisStorageContext storageContext,
            AdCheckDto.FileCheckRes response
    ) {
        AdCheckFileStorageService.StoredFile finalResult =
                uploadJsonArtifact(storageContext, FINAL_RESULT_PATH, response);
        return response.toBuilder()
                .finalResultObjectKey(finalResult.getObjectKey())
                .finalResultUrl(finalResult.getViewUrl())
                .build();
    }

    private AdCheckFileStorageService.StoredFile uploadJsonArtifact(
            AdCheckFileStorageService.AnalysisStorageContext storageContext,
            String relativePath,
            Object value
    ) {
        return adCheckFileStorageService.uploadJson(storageContext, relativePath, toJson(value));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("S3 artifact JSON cannot be serialized.", e);
        }
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }

    public static class FileCheckException extends RuntimeException {
        private final AdCheckDto.FileCheckRes response;

        public FileCheckException(String message, AdCheckDto.FileCheckRes response, Throwable cause) {
            super(message, cause);
            this.response = response;
        }

        public AdCheckDto.FileCheckRes getResponse() {
            return response;
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
