package org.example.backend.adcheck.analysis.service;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.example.backend.adcheck.model.AdCheckDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class AdCheckAnalysisMongoStorageService {
    private static final String DEFAULT_COLLECTION = "ad_check_analysis_results";

    @Value("${custom.mongodb.analysis-uri:}")
    private String analysisUri;

    @Value("${custom.mongodb.analysis-enabled:false}")
    private boolean analysisEnabled;

    @Value("${custom.mongodb.analysis-database:}")
    private String analysisDatabase;

    @Value("${custom.mongodb.analysis-collection:" + DEFAULT_COLLECTION + "}")
    private String analysisCollection;

    private MongoClient mongoClient;
    private String resolvedDatabaseName;

    public void save(AdCheckDto.FileCheckRes response) {
        if (!isEnabled() || response == null || !StringUtils.hasText(response.getAnalysisJobId())) {
            return;
        }

        try {
            saveInternal(response);
        } catch (Exception e) {
            log.warn("Ad check analysis MongoDB save failed. analysisJobId={}, errorType={}, message={}",
                    response.getAnalysisJobId(), e.getClass().getSimpleName(), sanitize(e.getMessage()));
        }
    }

    public String saveDetailOrThrow(AdCheckDto.FileCheckRes response) {
        if (response == null || !StringUtils.hasText(response.getAnalysisJobId())) {
            throw new IllegalArgumentException("analysisJobId is required to save ad check detail.");
        }
        if (!isEnabled()) {
            throw new IllegalStateException("Ad check analysis MongoDB is disabled.");
        }

        saveInternal(response);
        return response.getAnalysisJobId();
    }

    public Optional<AdCheckDto.FileCheckRes> findDetail(String documentId) {
        return findDocument(documentId).map(this::fromDocument);
    }

    public Optional<AdCheckDto.FileCheckRes> findDetailByJobId(String jobId) {
        return findDocumentByJobId(jobId).map(this::fromDocument);
    }

    public Optional<Map<String, Object>> findRawDocument(String documentId) {
        return findDocument(documentId).map(document -> new LinkedHashMap<>(document));
    }

    public Optional<Map<String, Object>> findRawDocumentByJobId(String jobId) {
        return findDocumentByJobId(jobId).map(document -> new LinkedHashMap<>(document));
    }

    private boolean isEnabled() {
        return analysisEnabled && StringUtils.hasText(analysisUri);
    }

    private synchronized MongoCollection<Document> collection() {
        if (mongoClient == null) {
            ConnectionString connectionString = new ConnectionString(analysisUri);
            String databaseName = StringUtils.hasText(analysisDatabase)
                    ? analysisDatabase.trim()
                    : connectionString.getDatabase();
            if (!StringUtils.hasText(databaseName)) {
                throw new IllegalStateException("custom.mongodb.analysis-database is required when URI has no database.");
            }

            mongoClient = MongoClients.create(connectionString);
            resolvedDatabaseName = databaseName;
        }

        String collectionName = StringUtils.hasText(analysisCollection)
                ? analysisCollection.trim()
                : DEFAULT_COLLECTION;
        return mongoClient.getDatabase(resolvedDatabaseName).getCollection(collectionName);
    }

    private void saveInternal(AdCheckDto.FileCheckRes response) {
        MongoCollection<Document> collection = collection();
        Document document = toDocument(response);
        collection.replaceOne(
                new Document("_id", response.getAnalysisJobId()),
                document,
                new ReplaceOptions().upsert(true)
        );
        log.info("Saved ad check analysis result to MongoDB. analysisJobId={}, database={}, collection={}",
                response.getAnalysisJobId(), resolvedDatabaseName, analysisCollection);
    }

    private Optional<Document> findDocument(String documentId) {
        if (!isEnabled() || !StringUtils.hasText(documentId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(collection().find(Filters.eq("_id", documentId.trim())).first());
    }

    private Optional<Document> findDocumentByJobId(String jobId) {
        if (!isEnabled() || !StringUtils.hasText(jobId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(collection().find(Filters.eq("jobId", jobId.trim())).first());
    }

    private Document toDocument(AdCheckDto.FileCheckRes response) {
        Date now = Date.from(Instant.now());
        String jobId = textValue(response.getContext(), "adCheckJobId");
        return new Document("_id", response.getAnalysisJobId())
                .append("jobId", jobId)
                .append("analysisJobId", response.getAnalysisJobId())
                .append("analysisObjectPrefix", response.getAnalysisObjectPrefix())
                .append("originalFileKey", response.getFileObjectKey())
                .append("originalFileName", response.getFileName())
                .append("originalContentType", response.getFileContentType())
                .append("originalFileUrl", response.getFileUrl())
                .append("file", fileDocument(response))
                .append("artifacts", artifactDocument(response))
                .append("documentStructureResult", documentStructureDocument(response))
                .append("result", resultDocument(response))
                .append("recognizedTextResult", recognizedTextDocument(response))
                .append("textRiskAnalysisResult", resultDocument(response))
                .append("finalResult", resultDocument(response))
                .append("errorDetail", response.getErrorMessage())
                .append("extractionMode", response.getExtractionMode())
                .append("extractedText", response.getExtractedText())
                .append("processingTimes", processingTimesDocument(response.getProcessingTimes()))
                .append("context", response.getContext() == null ? Map.of() : response.getContext())
                .append("createdAt", now)
                .append("updatedAt", now);
    }

    private Document fileDocument(AdCheckDto.FileCheckRes response) {
        return new Document("name", response.getFileName())
                .append("objectKey", response.getFileObjectKey())
                .append("url", response.getFileUrl())
                .append("contentType", response.getFileContentType())
                .append("size", response.getFileSize());
    }

    private Document artifactDocument(AdCheckDto.FileCheckRes response) {
        return new Document("extractedTextObjectKey", response.getExtractedTextObjectKey())
                .append("extractedTextUrl", response.getExtractedTextUrl())
                .append("aiResultObjectKey", response.getAiResultObjectKey())
                .append("aiResultUrl", response.getAiResultUrl())
                .append("finalResultObjectKey", response.getFinalResultObjectKey())
                .append("finalResultUrl", response.getFinalResultUrl())
                .append("images", imageDocuments(response.getExtractedImageAssets()));
    }

    private List<Document> imageDocuments(List<AdCheckDto.FileArtifact> imageAssets) {
        if (imageAssets == null || imageAssets.isEmpty()) {
            return List.of();
        }

        List<Document> documents = new ArrayList<>();
        for (AdCheckDto.FileArtifact image : imageAssets) {
            documents.add(new Document("type", image.getType())
                    .append("targetId", image.getTargetId())
                    .append("page", image.getPage())
                    .append("readingOrder", image.getReadingOrder())
                    .append("objectKey", image.getObjectKey())
                    .append("url", image.getUrl())
                    .append("contentType", image.getContentType())
                    .append("size", image.getFileSize()));
        }
        return documents;
    }

    private Document resultDocument(AdCheckDto.FileCheckRes response) {
        return new Document("status", response.getStatus())
                .append("law", response.getLaw())
                .append("violationText", response.getViolationText())
                .append("reason", response.getReason())
                .append("suggestion", response.getSuggestion())
                .append("errorMessage", response.getErrorMessage());
    }

    private Document recognizedTextDocument(AdCheckDto.FileCheckRes response) {
        return new Document("extractedText", response.getExtractedText())
                .append("extractionMode", response.getExtractionMode())
                .append("images", imageDocuments(response.getExtractedImageAssets()));
    }

    private Document documentStructureDocument(AdCheckDto.FileCheckRes response) {
        return new Document("extractionMode", response.getExtractionMode())
                .append("analysisObjectPrefix", response.getAnalysisObjectPrefix())
                .append("layoutMillis", response.getProcessingTimes() == null
                        ? null
                        : response.getProcessingTimes().getLayoutMillis())
                .append("extractedImageCount", response.getExtractedImageAssets() == null
                        ? 0
                        : response.getExtractedImageAssets().size());
    }

    private Document processingTimesDocument(AdCheckDto.ProcessingTimes processingTimes) {
        if (processingTimes == null) {
            return new Document();
        }

        return new Document("textExtractionMillis", processingTimes.getTextExtractionMillis())
                .append("layoutMillis", processingTimes.getLayoutMillis())
                .append("ocrMillis", processingTimes.getOcrMillis())
                .append("aiAnalysisMillis", processingTimes.getAiAnalysisMillis())
                .append("totalMillis", processingTimes.getTotalMillis());
    }

    private AdCheckDto.FileCheckRes fromDocument(Document document) {
        Document file = document.get("file", Document.class);
        Document artifacts = document.get("artifacts", Document.class);
        Document result = document.get("result", Document.class);

        return AdCheckDto.FileCheckRes.builder()
                .analysisJobId(text(document, "analysisJobId"))
                .analysisObjectPrefix(text(document, "analysisObjectPrefix"))
                .fileName(text(file, "name"))
                .fileObjectKey(text(file, "objectKey"))
                .fileUrl(text(file, "url"))
                .fileContentType(text(file, "contentType"))
                .fileSize(longValue(file == null ? null : file.get("size")))
                .extractedTextObjectKey(text(artifacts, "extractedTextObjectKey"))
                .extractedTextUrl(text(artifacts, "extractedTextUrl"))
                .aiResultObjectKey(text(artifacts, "aiResultObjectKey"))
                .aiResultUrl(text(artifacts, "aiResultUrl"))
                .finalResultObjectKey(text(artifacts, "finalResultObjectKey"))
                .finalResultUrl(text(artifacts, "finalResultUrl"))
                .extractedImageAssets(imageDtos(artifacts))
                .extractedText(text(document, "extractedText"))
                .status(text(result, "status"))
                .law(text(result, "law"))
                .violationText(text(result, "violationText"))
                .reason(text(result, "reason"))
                .suggestion(text(result, "suggestion"))
                .extractionMode(text(document, "extractionMode"))
                .processingTimes(processingTimesDto(document.get("processingTimes", Document.class)))
                .errorMessage(text(result, "errorMessage"))
                .build();
    }

    private List<AdCheckDto.FileArtifact> imageDtos(Document artifacts) {
        if (artifacts == null) {
            return List.of();
        }
        List<?> rawImages = artifacts.getList("images", Object.class, List.of());
        if (rawImages.isEmpty()) {
            return List.of();
        }

        List<AdCheckDto.FileArtifact> images = new ArrayList<>();
        for (Object rawImage : rawImages) {
            if (rawImage instanceof Document image) {
                images.add(AdCheckDto.FileArtifact.builder()
                        .type(text(image, "type"))
                        .targetId(text(image, "targetId"))
                        .page(intValue(image.get("page")))
                        .readingOrder(intValue(image.get("readingOrder")))
                        .objectKey(text(image, "objectKey"))
                        .url(text(image, "url"))
                        .contentType(text(image, "contentType"))
                        .fileSize(longValue(image.get("size")))
                        .build());
            }
        }
        return images;
    }

    private AdCheckDto.ProcessingTimes processingTimesDto(Document processingTimes) {
        if (processingTimes == null) {
            return null;
        }
        return AdCheckDto.ProcessingTimes.builder()
                .textExtractionMillis(longValue(processingTimes.get("textExtractionMillis")))
                .layoutMillis(longValue(processingTimes.get("layoutMillis")))
                .ocrMillis(longValue(processingTimes.get("ocrMillis")))
                .aiAnalysisMillis(longValue(processingTimes.get("aiAnalysisMillis")))
                .totalMillis(longValue(processingTimes.get("totalMillis")))
                .build();
    }

    private String text(Document document, String key) {
        if (document == null || key == null) {
            return null;
        }
        Object value = document.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String textValue(Map<String, Object> context, String key) {
        if (context == null || key == null) {
            return null;
        }
        Object value = context.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String sanitize(String message) {
        if (message == null) {
            return "";
        }
        return message.replaceAll("mongodb://[^\\s]+", "mongodb://<redacted>");
    }

    @PreDestroy
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
