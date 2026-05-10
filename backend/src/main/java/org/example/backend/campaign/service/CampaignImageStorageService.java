package org.example.backend.campaign.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 캠페인 썸네일 S3 저장.
 * - presigned PUT URL 발급 → frontend가 직접 S3에 업로드 → confirm 시 objectKey 저장.
 * - presigned GET URL 발급 → 디렉토리 카드 노출.
 * - AI 자동 생성 (Phase 4) 시 byte[] 직접 putObject 도 지원.
 */
@Service
@RequiredArgsConstructor
public class CampaignImageStorageService {

    private static final String CAMPAIGN_THUMB_PREFIX = "campaign-thumb/";
    private static final String UPLOADED_DIR = "uploaded";
    private static final String GENERATED_DIR = "generated";
    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;
    private static final long UPLOAD_EXPIRES_IN_SECONDS = 300L;
    private static final long VIEW_EXPIRES_IN_SECONDS = 900L;

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/webp", "webp"
    );
    private static final DateTimeFormatter OBJECT_KEY_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").withZone(ZoneOffset.UTC);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /** 사용자 업로드용 presigned PUT URL. frontend가 받은 url로 직접 S3 PUT. */
    public UploadUrlResult createUploadUrl(Long campaignId, String contentType, Long fileSize) {
        String normalized = normalizeContentType(contentType);
        resolveFileSize(fileSize);
        String objectKey = uploadedKey(campaignId, normalized);

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(normalized)
                .build();
        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(UPLOAD_EXPIRES_IN_SECONDS))
                .putObjectRequest(putReq)
                .build();
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignReq);
        return new UploadUrlResult(objectKey, presigned.url().toString(), UPLOAD_EXPIRES_IN_SECONDS, normalized);
    }

    /** AI 자동 생성 (Phase 4) — byte[] 직접 S3 업로드. */
    public String uploadGeneratedImage(Long campaignId, byte[] imageBytes, String contentType) {
        String normalized = normalizeContentType(contentType);
        if (imageBytes == null || imageBytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "generated thumbnail is empty.");
        }
        String objectKey = generatedKey(campaignId, normalized);
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .contentType(normalized)
                        .build(),
                RequestBody.fromBytes(imageBytes));
        return objectKey;
    }

    /** S3 업로드 완료 확인 (objectKey가 실제 S3에 존재하고 size·content-type 정상인지). */
    public void validateUploadedObject(String objectKey) {
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
            normalizeContentType(response.contentType());
            resolveFileSize(response.contentLength());
        } catch (S3Exception ex) {
            if (ex.statusCode() == HttpStatus.NOT_FOUND.value()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "thumbnail was not uploaded.");
            }
            throw ex;
        }
    }

    /** 디렉토리 카드 노출용 — presigned GET URL 발급 (15분 유효). */
    public String createViewUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return null;
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(VIEW_EXPIRES_IN_SECONDS))
                .getObjectRequest(getReq)
                .build();
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignReq);
        return presigned.url().toString();
    }

    public void deleteObject(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return;
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(objectKey).build());
    }

    public boolean isCampaignThumbKey(Long campaignId, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return false;
        String prefix = CAMPAIGN_THUMB_PREFIX + campaignId + "/";
        return objectKey.startsWith(prefix);
    }

    // ── Helpers ─────────────────────────────────────────────

    private String uploadedKey(Long campaignId, String contentType) {
        return CAMPAIGN_THUMB_PREFIX + campaignId + "/" + UPLOADED_DIR + "/"
                + uniqueFileName(EXTENSION_BY_CONTENT_TYPE.get(contentType));
    }

    private String generatedKey(Long campaignId, String contentType) {
        return CAMPAIGN_THUMB_PREFIX + campaignId + "/" + GENERATED_DIR + "/"
                + uniqueFileName(EXTENSION_BY_CONTENT_TYPE.get(contentType));
    }

    private String uniqueFileName(String extension) {
        return OBJECT_KEY_TIMESTAMP_FORMATTER.format(Instant.now()) + "_" + UUID.randomUUID() + "." + extension;
    }

    private String normalizeContentType(String contentType) {
        String v = String.valueOf(contentType).trim().toLowerCase();
        if (!EXTENSION_BY_CONTENT_TYPE.containsKey(v)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported thumbnail content type.");
        }
        return v;
    }

    private long resolveFileSize(Long fileSize) {
        long n = Objects.requireNonNullElse(fileSize, 0L);
        if (n <= 0 || n > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "thumbnail size must be 1 byte to 5MB.");
        }
        return n;
    }

    public record UploadUrlResult(String objectKey, String uploadUrl, long expiresInSeconds, String contentType) {}
}
