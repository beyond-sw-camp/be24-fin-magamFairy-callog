package com.example.adcheck.campaign;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CampaignProjectionMongoService {
    private static final String DEFAULT_COLLECTION = "campaign_projection";

    @Value("${custom.mongodb.campaign-enabled:false}")
    private boolean enabled;

    @Value("${custom.mongodb.campaign-uri:${custom.mongodb.analysis-uri:}}")
    private String mongoUri;

    @Value("${custom.mongodb.campaign-database:${custom.mongodb.analysis-database:}}")
    private String mongoDatabase;

    @Value("${custom.mongodb.campaign-collection:" + DEFAULT_COLLECTION + "}")
    private String collectionName;

    @Value("${custom.mongodb.analysis-server-selection-timeout-ms:5000}")
    private long serverSelectionTimeoutMillis;

    @Value("${custom.mongodb.analysis-connect-timeout-ms:5000}")
    private long connectTimeoutMillis;

    @Value("${custom.mongodb.analysis-read-timeout-ms:10000}")
    private long readTimeoutMillis;

    private MongoClient mongoClient;
    private String resolvedDatabaseName;

    public boolean isEnabled() {
        return enabled && StringUtils.hasText(mongoUri);
    }

    public void upsertCampaign(Map<String, Object> campaign) {
        if (!isEnabled() || campaign == null) {
            return;
        }

        String campaignId = text(campaign.get("campaignId"));
        if (!StringUtils.hasText(campaignId)) {
            return;
        }

        Date now = Date.from(Instant.now());
        collection().updateOne(
                Filters.eq("_id", campaignId),
                Updates.combine(
                        Updates.set("campaignId", campaignId),
                        Updates.set("campaignIdx", campaign.get("campaignIdx")),
                        Updates.set("ownerLoginId", campaign.get("ownerLoginId")),
                        Updates.set("name", campaign.get("name")),
                        Updates.set("status", campaign.get("status")),
                        Updates.set("visibility", campaign.get("visibility")),
                        Updates.set("startDate", campaign.get("startDate")),
                        Updates.set("endDate", campaign.get("endDate")),
                        Updates.set("updatedAt", now),
                        Updates.setOnInsert("createdAt", now),
                        Updates.setOnInsert("members", new ArrayList<>())
                ),
                new UpdateOptions().upsert(true)
        );
        log.info("Upserted campaign projection. campaignId={}", campaignId);
    }

    public void upsertMember(String campaignId, Map<String, Object> member) {
        if (!isEnabled() || !StringUtils.hasText(campaignId) || member == null) {
            return;
        }

        Long userIdx = longValue(member.get("userIdx"));
        if (userIdx == null) {
            return;
        }

        Date now = Date.from(Instant.now());
        Document memberDocument = new Document("userIdx", userIdx)
                .append("loginId", text(member.get("loginId")))
                .append("email", text(member.get("email")))
                .append("name", text(member.get("name")))
                .append("role", text(member.get("role")))
                .append("status", "ACTIVE")
                .append("joinedAt", member.get("joinedAt"))
                .append("updatedAt", now);

        ensureCampaignDocument(campaignId, now);
        collection().updateOne(
                Filters.eq("_id", campaignId),
                Updates.pull("members", new Document("userIdx", userIdx))
        );
        collection().updateOne(
                Filters.eq("_id", campaignId),
                Updates.combine(
                        Updates.push("members", memberDocument),
                        Updates.set("updatedAt", now)
                )
        );
        log.info("Upserted campaign member projection. campaignId={}, userIdx={}", campaignId, userIdx);
    }

    public void removeMember(String campaignId, Map<String, Object> member) {
        if (!isEnabled() || !StringUtils.hasText(campaignId) || member == null) {
            return;
        }

        Long userIdx = longValue(member.get("userIdx"));
        if (userIdx == null) {
            return;
        }

        collection().updateOne(
                Filters.eq("_id", campaignId),
                Updates.combine(
                        Updates.pull("members", new Document("userIdx", userIdx)),
                        Updates.set("updatedAt", Date.from(Instant.now()))
                )
        );
        log.info("Removed campaign member projection. campaignId={}, userIdx={}", campaignId, userIdx);
    }

    public boolean hasActiveMember(String campaignId, Long userIdx) {
        if (!isEnabled()) {
            throw new IllegalStateException("campaign projection is disabled.");
        }
        if (!StringUtils.hasText(campaignId) || userIdx == null) {
            return false;
        }

        Document document = collection().find(Filters.and(
                Filters.eq("_id", campaignId.trim()),
                Filters.elemMatch("members", Filters.and(
                        Filters.eq("userIdx", userIdx),
                        Filters.eq("status", "ACTIVE")
                ))
        )).first();
        return document != null;
    }

    private void ensureCampaignDocument(String campaignId, Date now) {
        collection().updateOne(
                Filters.eq("_id", campaignId),
                Updates.combine(
                        Updates.setOnInsert("_id", campaignId),
                        Updates.setOnInsert("campaignId", campaignId),
                        Updates.setOnInsert("createdAt", now),
                        Updates.setOnInsert("members", new ArrayList<>()),
                        Updates.set("updatedAt", now)
                ),
                new UpdateOptions().upsert(true)
        );
    }

    private synchronized MongoCollection<Document> collection() {
        if (mongoClient == null) {
            ConnectionString connectionString = new ConnectionString(mongoUri);
            String databaseName = StringUtils.hasText(mongoDatabase)
                    ? mongoDatabase.trim()
                    : connectionString.getDatabase();
            if (!StringUtils.hasText(databaseName)) {
                throw new IllegalStateException("custom.mongodb.campaign-database is required when URI has no database.");
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

        String resolvedCollection = StringUtils.hasText(collectionName)
                ? collectionName.trim()
                : DEFAULT_COLLECTION;
        return mongoClient.getDatabase(resolvedDatabaseName).getCollection(resolvedCollection);
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            String raw = text(value);
            return raw == null || raw.isBlank() ? null : Long.parseLong(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @PreDestroy
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
