package com.example.adcheck.service;

import com.example.adcheck.analysis.service.AdCheckAnalysisMongoStorageService;
import com.example.adcheck.model.AdCheckDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class AiJudgeFileCheckService {
    private static final String EXTRACTED_TEXT_PATH = "ocr/extracted-text.txt";
    private static final String AI_RESULT_PATH = "ai/result.json";
    private static final String AI_ERROR_PATH = "ai/error.json";
    private static final String FINAL_RESULT_PATH = "final/result.json";

    private final AiJudgeService aiJudgeService;
    private final ObjectMapper objectMapper;
    private final TextExtractorService textExtractorService;
    private final AdCheckFileStorageService adCheckFileStorageService;
    private final AdCheckAnalysisMongoStorageService adCheckAnalysisMongoStorageService;
    private final AiJudgeKafkaEventPublisher aiJudgeKafkaEventPublisher;

    public AiJudgeFileCheckService(
            AiJudgeService aiJudgeService,
            ObjectMapper objectMapper,
            TextExtractorService textExtractorService,
            AdCheckFileStorageService adCheckFileStorageService,
            AdCheckAnalysisMongoStorageService adCheckAnalysisMongoStorageService,
            AiJudgeKafkaEventPublisher aiJudgeKafkaEventPublisher
    ) {
        this.aiJudgeService = aiJudgeService;
        this.objectMapper = objectMapper;
        this.textExtractorService = textExtractorService;
        this.adCheckFileStorageService = adCheckFileStorageService;
        this.adCheckAnalysisMongoStorageService = adCheckAnalysisMongoStorageService;
        this.aiJudgeKafkaEventPublisher = aiJudgeKafkaEventPublisher;
    }

    public AdCheckDto.FileCheckRes checkFile(MultipartFile file) {
        return checkFile(file, null);
    }

    public AdCheckDto.FileCheckRes checkFile(MultipartFile file, String rawContext) {
        long totalStartedAt = System.nanoTime();
        Map<String, Object> context = parseContext(rawContext);
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
                AdCheckDto.Res result = aiJudgeService.check(extraction.text());
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
                        null,
                        context
                );
                response = storeFinalResult(storageContext, response);
                adCheckAnalysisMongoStorageService.save(response);
                aiJudgeKafkaEventPublisher.publishCompleted(response);
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
                        e.getMessage(),
                        context
                );
                partialResponse = storeFinalResult(storageContext, partialResponse);
                adCheckAnalysisMongoStorageService.save(partialResponse);
                aiJudgeKafkaEventPublisher.publishFailed(partialResponse);
                throw new FileCheckException(e.getMessage(), partialResponse, e);
            }
        } catch (IOException e) {
            throw new RuntimeException("File processing failed.", e);
        }
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
            String errorMessage,
            Map<String, Object> context
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
                .errorMessage(errorMessage)
                .context(context == null ? Map.of() : Map.copyOf(context));

        if (result != null) {
            builder.status(result.status())
                    .law(result.law())
                    .violationText(result.violationText())
                    .reason(result.reason())
                    .suggestion(result.suggestion());
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

    private Map<String, Object> parseContext(String rawContext) {
        if (!hasText(rawContext)) {
            return Map.of();
        }

        try {
            Map<String, Object> context = objectMapper.readValue(rawContext, new TypeReference<Map<String, Object>>() {});
            return context == null ? Map.of() : context;
        } catch (Exception e) {
            log.warn("AI judge context cannot be parsed. context={}", rawContext, e);
            return Map.of();
        }
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
}
