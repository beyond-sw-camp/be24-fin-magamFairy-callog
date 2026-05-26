package com.example.adcheck.analysis.service;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import com.example.adcheck.service.AdCheckFileStorageService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import com.example.adcheck.model.AdCheckDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

    @Value("${custom.mongodb.analysis-server-selection-timeout-ms:5000}")
    private long serverSelectionTimeoutMillis;

    @Value("${custom.mongodb.analysis-connect-timeout-ms:5000}")
    private long connectTimeoutMillis;

    @Value("${custom.mongodb.analysis-read-timeout-ms:10000}")
    private long readTimeoutMillis;

    private MongoClient mongoClient;
    private String resolvedDatabaseName;
    private final AdCheckFileStorageService adCheckFileStorageService;

    public AdCheckAnalysisMongoStorageService(AdCheckFileStorageService adCheckFileStorageService) {
        this.adCheckFileStorageService = adCheckFileStorageService;
    }

    public boolean save(AdCheckDto.FileCheckRes response) {
        if (!isEnabled() || response == null || !StringUtils.hasText(response.getAnalysisJobId())) {
            return false;
        }

        try {
            MongoCollection<Document> collection = collection();
            Document document = toDocument(response);
            collection.replaceOne(
                    new Document("_id", response.getAnalysisJobId()),
                    document,
                    new ReplaceOptions().upsert(true)
            );
            log.info("Saved ad check analysis result to MongoDB. analysisJobId={}, database={}, collection={}",
                    response.getAnalysisJobId(), resolvedDatabaseName, analysisCollection);
            return true;
        } catch (Exception e) {
            log.warn("Ad check analysis MongoDB save failed. analysisJobId={}, errorType={}, message={}",
                    response.getAnalysisJobId(), e.getClass().getSimpleName(), sanitize(e.getMessage()));
            return false;
        }
    }

    public Optional<AdCheckDto.FileCheckRes> findByAnalysisJobId(String analysisJobId) {
        if (!isEnabled() || !StringUtils.hasText(analysisJobId)) {
            return Optional.empty();
        }

        Document document = collection().find(new Document("_id", analysisJobId.trim())).first();
        if (document == null) {
            return Optional.empty();
        }
        return Optional.of(toResponse(document));
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

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .applyToClusterSettings(builder -> builder.serverSelectionTimeout(
                            Math.max(1000L, serverSelectionTimeoutMillis),
                            TimeUnit.MILLISECONDS
                    ))
                    .applyToSocketSettings(builder -> builder
                            .connectTimeout(Math.max(1000L, connectTimeoutMillis), TimeUnit.MILLISECONDS)
                            .readTimeout(Math.max(1000L, readTimeoutMillis), TimeUnit.MILLISECONDS))
                    .build();

            mongoClient = MongoClients.create(settings);
            resolvedDatabaseName = databaseName;
        }

        String collectionName = StringUtils.hasText(analysisCollection)
                ? analysisCollection.trim()
                : DEFAULT_COLLECTION;
        return mongoClient.getDatabase(resolvedDatabaseName).getCollection(collectionName);
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
                .append("verdictLevel", response.getVerdictLevel())
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

    private AdCheckDto.FileCheckRes toResponse(Document document) {
        Document file = document.get("file", Document.class);
        Document artifacts = document.get("artifacts", Document.class);
        Document result = document.get("result", Document.class);
        Document processingTimes = document.get("processingTimes", Document.class);

        return AdCheckDto.FileCheckRes.builder()
                .analysisJobId(text(document, "analysisJobId"))
                .analysisObjectPrefix(text(document, "analysisObjectPrefix"))
                .fileName(text(file, "name"))
                .fileObjectKey(text(file, "objectKey"))
                .fileUrl(viewUrl(text(file, "objectKey"), text(file, "contentType")))
                .fileContentType(text(file, "contentType"))
                .fileSize(longValue(file, "size"))
                .extractedTextObjectKey(text(artifacts, "extractedTextObjectKey"))
                .extractedTextUrl(viewUrl(text(artifacts, "extractedTextObjectKey"), "text/plain"))
                .aiResultObjectKey(text(artifacts, "aiResultObjectKey"))
                .aiResultUrl(viewUrl(text(artifacts, "aiResultObjectKey"), "application/json"))
                .finalResultObjectKey(text(artifacts, "finalResultObjectKey"))
                .finalResultUrl(viewUrl(text(artifacts, "finalResultObjectKey"), "application/json"))
                .extractedImageAssets(imageArtifacts(artifacts))
                .extractedText(text(document, "extractedText"))
                .status(text(result, "status"))
                .law(text(result, "law"))
                .violationText(text(result, "violationText"))
                .reason(text(result, "reason"))
                .suggestion(text(result, "suggestion"))
                .verdictLevel(integerValue(result, "verdictLevel"))
                .errorMessage(text(result, "errorMessage"))
                .extractionMode(text(document, "extractionMode"))
                .processingTimes(toProcessingTimes(processingTimes))
                .build();
    }

    private List<AdCheckDto.FileArtifact> imageArtifacts(Document artifacts) {
        if (artifacts == null) {
            return List.of();
        }
        List<Document> images = artifacts.getList("images", Document.class, List.of());
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        List<AdCheckDto.FileArtifact> imageArtifacts = new ArrayList<>();
        for (Document image : images) {
            imageArtifacts.add(AdCheckDto.FileArtifact.builder()
                    .type(text(image, "type"))
                    .targetId(text(image, "targetId"))
                    .page(integerValue(image, "page"))
                    .readingOrder(integerValue(image, "readingOrder"))
                    .objectKey(text(image, "objectKey"))
                    .url(viewUrl(text(image, "objectKey"), text(image, "contentType")))
                    .contentType(text(image, "contentType"))
                    .fileSize(longValue(image, "size"))
                    .build());
        }
        return imageArtifacts;
    }

    private AdCheckDto.ProcessingTimes toProcessingTimes(Document processingTimes) {
        if (processingTimes == null || processingTimes.isEmpty()) {
            return null;
        }
        return AdCheckDto.ProcessingTimes.builder()
                .textExtractionMillis(longValue(processingTimes, "textExtractionMillis"))
                .layoutMillis(longValue(processingTimes, "layoutMillis"))
                .ocrMillis(longValue(processingTimes, "ocrMillis"))
                .aiAnalysisMillis(longValue(processingTimes, "aiAnalysisMillis"))
                .totalMillis(longValue(processingTimes, "totalMillis"))
                .build();
    }

    private String text(Document document, String name) {
        if (document == null) {
            return null;
        }
        Object value = document.get(name);
        return value == null ? null : String.valueOf(value);
    }

    private Integer integerValue(Document document, String name) {
        Number value = number(document, name);
        return value == null ? null : value.intValue();
    }

    private Long longValue(Document document, String name) {
        Number value = number(document, name);
        return value == null ? null : value.longValue();
    }

    private Number number(Document document, String name) {
        if (document == null) {
            return null;
        }
        Object value = document.get(name);
        return value instanceof Number number ? number : null;
    }

    private String viewUrl(String objectKey, String contentType) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        return adCheckFileStorageService.createViewUrl(objectKey, contentType);
    }

    private String sanitize(String message) {
        if (message == null) {
            return "";
        }
        return message.replaceAll("mongodb://[^\\s]+", "mongodb://<redacted>");
    }

    private String textValue(Map<String, Object> context, String key) {
        if (context == null || key == null) {
            return null;
        }
        Object value = context.get(key);
        return value == null ? null : String.valueOf(value);
    }

    @PreDestroy
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
