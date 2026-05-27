package org.example.backend.campaign.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignMember;
import org.example.backend.user.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignKafkaEventPublisher {
    public static final String CAMPAIGN_UPSERTED = "CAMPAIGN_UPSERTED";
    public static final String CAMPAIGN_MEMBER_UPSERTED = "CAMPAIGN_MEMBER_UPSERTED";
    public static final String CAMPAIGN_MEMBER_REMOVED = "CAMPAIGN_MEMBER_REMOVED";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.campaign-sync-enabled:false}")
    private boolean enabled;

    @Value("${app.kafka.topics.campaign-sync:campaign.sync}")
    private String campaignSyncTopic;

    public void publishCampaignUpserted(Campaign campaign) {
        if (campaign == null) {
            return;
        }

        publish(campaign.getPublicId(), eventBase(CAMPAIGN_UPSERTED, campaign)
                .append("campaign", campaignPayload(campaign)));
    }

    public void publishMemberUpserted(CampaignMember member) {
        if (member == null || member.getCampaign() == null || member.getUser() == null) {
            return;
        }

        publish(member.getCampaign().getPublicId(), eventBase(CAMPAIGN_MEMBER_UPSERTED, member.getCampaign())
                .append("member", memberPayload(member)));
    }

    public void publishMemberRemoved(Campaign campaign, Long userIdx) {
        if (campaign == null || userIdx == null) {
            return;
        }

        publish(campaign.getPublicId(), eventBase(CAMPAIGN_MEMBER_REMOVED, campaign)
                .append("member", new EventMap()
                        .append("userIdx", userIdx)
                        .append("status", "REMOVED")));
    }

    private EventMap eventBase(String eventType, Campaign campaign) {
        return new EventMap()
                .append("eventId", UUID.randomUUID().toString())
                .append("eventType", eventType)
                .append("occurredAt", Instant.now().toString())
                .append("campaignId", campaign == null ? null : campaign.getPublicId())
                .append("campaignIdx", campaign == null ? null : campaign.getIdx())
                .append("producer", "app");
    }

    private EventMap campaignPayload(Campaign campaign) {
        return new EventMap()
                .append("campaignId", campaign.getPublicId())
                .append("campaignIdx", campaign.getIdx())
                .append("ownerLoginId", campaign.getOwnerLoginId())
                .append("name", campaign.getName())
                .append("status", campaign.getStatus())
                .append("visibility", campaign.getVisibility())
                .append("startDate", campaign.getStartDate())
                .append("endDate", campaign.getEndDate());
    }

    private EventMap memberPayload(CampaignMember member) {
        User user = member.getUser();
        return new EventMap()
                .append("userIdx", user.getIdx())
                .append("loginId", user.getId())
                .append("email", user.getEmail())
                .append("name", user.getName())
                .append("role", member.getCampaignRole() == null ? null : member.getCampaignRole().name())
                .append("status", "ACTIVE")
                .append("joinedAt", member.getJoinedAt());
    }

    private void publish(String key, Map<String, Object> payload) {
        if (!enabled || key == null || key.isBlank() || payload == null) {
            return;
        }

        Runnable send = () -> {
            try {
                kafkaTemplate.send(campaignSyncTopic, key, objectMapper.writeValueAsString(payload));
                log.info("Published campaign sync event. topic={}, key={}, eventType={}",
                        campaignSyncTopic, key, payload.get("eventType"));
            } catch (Exception e) {
                log.warn("Failed to publish campaign sync event. topic={}, key={}, eventType={}",
                        campaignSyncTopic, key, payload.get("eventType"), e);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
            return;
        }

        send.run();
    }

    private static class EventMap extends LinkedHashMap<String, Object> {
        EventMap append(String key, Object value) {
            put(key, value);
            return this;
        }
    }
}
