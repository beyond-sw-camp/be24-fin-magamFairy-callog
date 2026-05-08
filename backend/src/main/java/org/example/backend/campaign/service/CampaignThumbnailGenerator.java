package org.example.backend.campaign.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.repository.CampaignRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 캠페인 썸네일 자동 생성 — `@Async`로 캠페인 생성 응답을 막지 않음.
 * 사용자가 업로드 안 한 경우 OpenAI Image API로 1536x1024 PNG 생성 → S3 업로드 → objectKey 저장.
 * 키 미설정·OpenAI 실패 시 조용히 skip (로그만).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignThumbnailGenerator {

    private final OpenAiCampaignThumbnailClient openAiClient;
    private final CampaignImageStorageService thumbnailStorage;
    private final CampaignRepository campaignRepository;

    @Async
    @Transactional
    public void generateAsyncIfMissing(Long campaignId) {
        if (!openAiClient.isConfigured()) {
            log.info("[thumb-gen] skip campaign={} — OpenAI key not configured", campaignId);
            return;
        }
        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign == null) return;
        if (campaign.getThumbnailObjectKey() != null && !campaign.getThumbnailObjectKey().isBlank()) {
            return;   // 이미 사용자가 업로드함
        }
        try {
            byte[] png = openAiClient.generateThumbnail(campaign);
            String key = thumbnailStorage.uploadGeneratedImage(campaign.getIdx(), png, "image/png");
            campaign.updateThumbnailObjectKey(key);
            log.info("[thumb-gen] generated campaign={} key={}", campaignId, key);
        } catch (Exception ex) {
            log.warn("[thumb-gen] failed campaign={}: {}", campaignId, ex.getMessage());
        }
    }
}
