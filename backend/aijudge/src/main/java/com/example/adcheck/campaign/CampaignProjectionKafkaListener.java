package com.example.adcheck.campaign;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignProjectionKafkaListener {
    private static final String CAMPAIGN_UPSERTED = "CAMPAIGN_UPSERTED";
    private static final String CAMPAIGN_MEMBER_UPSERTED = "CAMPAIGN_MEMBER_UPSERTED";
    private static final String CAMPAIGN_MEMBER_REMOVED = "CAMPAIGN_MEMBER_REMOVED";

    private final ObjectMapper objectMapper;
    private final CampaignProjectionMongoService campaignProjectionMongoService;

    @KafkaListener(
            topics = "${ai-judge.kafka.campaign-sync-topic:campaign.sync}",
            groupId = "${ai-judge.kafka.campaign-sync-group-id:ai-judge-campaign-projection}",
            autoStartup = "${ai-judge.kafka.campaign-sync-enabled:false}"
    )
    public void handleCampaignSyncEvent(String payload) {
        try {
            Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
            String eventType = text(event.get("eventType"));
            String campaignId = text(event.get("campaignId"));

            if (CAMPAIGN_UPSERTED.equals(eventType)) {
                campaignProjectionMongoService.upsertCampaign(mapValue(event.get("campaign")));
                return;
            }
            if (CAMPAIGN_MEMBER_UPSERTED.equals(eventType)) {
                campaignProjectionMongoService.upsertMember(campaignId, mapValue(event.get("member")));
                return;
            }
            if (CAMPAIGN_MEMBER_REMOVED.equals(eventType)) {
                campaignProjectionMongoService.removeMember(campaignId, mapValue(event.get("member")));
                return;
            }

            log.warn("Unsupported campaign sync event. eventType={}, campaignId={}", eventType, campaignId);
        } catch (Exception e) {
            log.warn("Campaign sync event cannot be processed. payload={}", payload, e);
            throw new IllegalArgumentException("Campaign sync event cannot be processed.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
