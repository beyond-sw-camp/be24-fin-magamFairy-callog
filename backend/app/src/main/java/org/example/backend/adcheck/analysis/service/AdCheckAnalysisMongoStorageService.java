package org.example.backend.adcheck.analysis.service;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
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
import java.util.List;

@Service
@Slf4j
public class AdCheckAnalysisMongoStorageService {
    private static final String DEFAULT_COLLECTION = "ad_check_analysis_results";

    @Value("${custom.mongodb.analysis-uri:}")
    private String analysisUri;

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
            MongoCollection<Document> collection = collection();
            Document document = toDocument(response);
            collection.replaceOne(
                    new Document("_id", response.getAnalysisJobId()),
                    document,
                    new ReplaceOptions().upsert(true)
            );
            log.info("Saved ad check analysis result to MongoDB. analysisJobId={}, database={}, collection={}",
                    response.getAnalysisJobId(), resolvedDatabaseName, analysisCollection);
        } catch (Exception e) {
            log.warn("Ad check analysis MongoDB save failed. analysisJobId={}, errorType={}, message={}",
                    response.getAnalysisJobId(), e.getClass().getSimpleName(), sanitize(e.getMessage()));
        }
    }

    private boolean isEnabled() {
        return StringUtils.hasText(analysisUri);
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

    private Document toDocument(AdCheckDto.FileCheckRes response) {
        Date now = Date.from(Instant.now());
        return new Document("_id", response.getAnalysisJobId())
                .append("analysisJobId", response.getAnalysisJobId())
                .append("analysisObjectPrefix", response.getAnalysisObjectPrefix())
                .append("file", fileDocument(response))
                .append("artifacts", artifactDocument(response))
                .append("result", resultDocument(response))
                .append("extractionMode", response.getExtractionMode())
                .append("extractedText", response.getExtractedText())
                .append("processingTimes", processingTimesDocument(response.getProcessingTimes()))
                .append("createdAt", now)
                .append("updatedAt", now);
    }

    private Document fileDocument(AdCheckDto.FileCheckRes response) {
        return new Document("name", response.getFileName())
                .append("objectKey", response.getFileObjectKey())
                .append("contentType", response.getFileContentType())
                .append("size", response.getFileSize());
    }

    private Document artifactDocument(AdCheckDto.FileCheckRes response) {
        return new Document("extractedTextObjectKey", response.getExtractedTextObjectKey())
                .append("aiResultObjectKey", response.getAiResultObjectKey())
                .append("finalResultObjectKey", response.getFinalResultObjectKey())
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
