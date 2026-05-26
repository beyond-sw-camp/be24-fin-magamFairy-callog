package com.example.adcheck.event;

import com.example.adcheck.campaign.CampaignProjectionMongoService;
import com.example.adcheck.model.AdCheckDto;
import com.example.adcheck.service.AdCheckFileStorageService;
import com.example.adcheck.service.AiJudgeFileCheckService;
import com.example.adcheck.service.AiJudgeKafkaEventPublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiJudgeKafkaRequestListener {
    private final ObjectMapper objectMapper;
    private final AiJudgeFileCheckService aiJudgeFileCheckService;
    private final AiJudgeKafkaEventPublisher aiJudgeKafkaEventPublisher;
    private final CampaignProjectionMongoService campaignProjectionMongoService;
    private final AdCheckFileStorageService adCheckFileStorageService;

    @KafkaListener(
            topics = "${ai-judge.kafka.request-topic:ai-judge.requests}",
            groupId = "${ai-judge.kafka.request-group-id:ai-judge-file-check}",
            autoStartup = "${ai-judge.kafka.enabled:false}"
    )
    public void handleFileCheckRequest(String payload) {
        String jobId = null;
        String fileName = null;
        String contentType = null;
        String analysisJobId = null;
        Map<String, Object> context = Map.of();

        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = text(event, "eventType");
            if (!"AI_JUDGE_FILE_CHECK_REQUEST".equals(eventType)) {
                log.warn("Skipped unsupported AI judge request event. eventType={}", eventType);
                return;
            }

            jobId = firstText(text(event, "jobId"), text(event, "analysisJobId"));
            analysisJobId = firstText(text(event, "analysisJobId"), jobId);
            fileName = firstText(text(event, "fileName"), "upload");
            contentType = text(event, "fileContentType");
            context = parseContext(event.path("context"));
            verifyCampaignAccess(context);

            byte[] fileBytes = resolveFilePayload(event);
            MultipartFile file = new StoredMultipartFile(fileName, contentType, fileBytes);

            log.info("Consumed AI judge file check request. jobId={}, fileName={}, size={}",
                    jobId, fileName, fileBytes.length);
            aiJudgeFileCheckService.checkFile(file, serializeContext(context), analysisJobId);
        } catch (AiJudgeFileCheckService.FileCheckException e) {
            log.warn("AI judge file check request failed with partial response. jobId={}, fileName={}",
                    jobId, fileName, e);
        } catch (Exception e) {
            log.warn("AI judge file check request failed. jobId={}, fileName={}", jobId, fileName, e);
            aiJudgeKafkaEventPublisher.publishFailed(buildFailedResponse(
                    firstText(analysisJobId, jobId),
                    fileName,
                    contentType,
                    context,
                    e
            ));
        }
    }

    private byte[] resolveFilePayload(JsonNode event) {
        String fileObjectKey = text(event, "fileObjectKey");
        if (!fileObjectKey.isBlank()) {
            return adCheckFileStorageService.downloadBytes(fileObjectKey);
        }

        String fileBase64 = text(event, "fileBase64");
        if (fileBase64.isBlank()) {
            throw new IllegalArgumentException("fileObjectKey or fileBase64 is required.");
        }
        return Base64.getDecoder().decode(fileBase64);
    }

    private void verifyCampaignAccess(Map<String, Object> context) {
        String campaignId = stringValue(context.get("campaignId"));
        if (campaignId == null || campaignId.isBlank()) {
            return;
        }

        Long requesterUserIdx = longValue(context.get("requesterUserIdx"));
        if (requesterUserIdx == null) {
            throw new IllegalArgumentException("requesterUserIdx is required.");
        }
        if (!campaignProjectionMongoService.isEnabled()) {
            log.warn("Skip AI judge campaign projection check because projection storage is disabled. campaignId={}, requesterUserIdx={}",
                    campaignId, requesterUserIdx);
            return;
        }
        if (!campaignProjectionMongoService.hasActiveMember(campaignId, requesterUserIdx)) {
            throw new IllegalArgumentException("campaign access denied.");
        }
    }

    private Map<String, Object> parseContext(JsonNode contextNode) {
        if (contextNode == null || contextNode.isMissingNode() || contextNode.isNull()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> context = objectMapper.convertValue(
                contextNode,
                new TypeReference<Map<String, Object>>() {}
        );
        return context == null ? new LinkedHashMap<>() : new LinkedHashMap<>(context);
    }

    private String serializeContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(context);
        } catch (Exception e) {
            throw new IllegalArgumentException("context cannot be serialized.", e);
        }
    }

    private AdCheckDto.FileCheckRes buildFailedResponse(
            String analysisJobId,
            String fileName,
            String contentType,
            Map<String, Object> context,
            Exception error
    ) {
        return AdCheckDto.FileCheckRes.builder()
                .analysisJobId(firstText(analysisJobId, stringValue(context.get("adCheckJobId"))))
                .fileName(firstText(fileName, "upload"))
                .fileContentType(contentType)
                .status("failed")
                .errorMessage(error == null ? "AI judge Kafka request failed." : error.getMessage())
                .context(context == null ? Map.of() : Map.copyOf(context))
                .build();
    }

    private String text(JsonNode node, String name) {
        JsonNode value = node == null ? null : node.path(name);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return "";
        }
        return value.asText("");
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            String raw = stringValue(value);
            return raw == null || raw.isBlank() ? null : Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("requesterUserIdx must be a number.", e);
        }
    }

    private static class StoredMultipartFile implements MultipartFile {
        private final String originalFilename;
        private final String contentType;
        private final byte[] bytes;

        private StoredMultipartFile(String originalFilename, String contentType, byte[] bytes) {
            this.originalFilename = originalFilename == null || originalFilename.isBlank() ? "upload" : originalFilename;
            this.contentType = contentType;
            this.bytes = bytes == null ? new byte[0] : bytes;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            org.springframework.util.FileCopyUtils.copy(bytes, dest);
        }
    }
}
