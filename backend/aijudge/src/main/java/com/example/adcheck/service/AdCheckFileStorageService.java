package com.example.adcheck.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdCheckFileStorageService {
    private static final String AD_CHECK_FILE_PREFIX = "adcheck";
    private static final long MAX_FILE_SIZE = 20L * 1024L * 1024L;
    private static final long VIEW_EXPIRES_IN_SECONDS = 900L;
    private static final DateTimeFormatter DATE_PATH_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").withZone(ZoneOffset.UTC);
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "text/plain", "txt",
            "application/pdf", "pdf",
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/webp", "webp",
            "image/bmp", "bmp",
            "image/tiff", "tiff",
            "application/json", "json"
    );
    private static final Map<String, String> CONTENT_TYPE_BY_EXTENSION = Map.of(
            "txt", "text/plain",
            "pdf", "application/pdf",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "webp", "image/webp",
            "bmp", "image/bmp",
            "tif", "image/tiff",
            "tiff", "image/tiff",
            "json", "application/json"
    );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public AnalysisStorageContext createAnalysisStorageContext(String originalFileName) {
        return createAnalysisStorageContext(originalFileName, null);
    }

    public AnalysisStorageContext createAnalysisStorageContext(String originalFileName, String requestedWorkId) {
        Instant now = Instant.now();
        String safeBaseName = sanitizeBaseName(removeExtension(resolveOriginalFileName(originalFileName)));
        String workId = requestedWorkId == null || requestedWorkId.isBlank()
                ? FILE_TIMESTAMP_FORMATTER.format(now)
                + "_"
                + UUID.randomUUID().toString().substring(0, 8)
                + "_"
                + safeBaseName
                : sanitizeBaseName(requestedWorkId);

        return AnalysisStorageContext.builder()
                .workId(workId)
                .basePrefix(AD_CHECK_FILE_PREFIX + "/" + DATE_PATH_FORMATTER.format(now) + "/" + workId)
                .build();
    }

    public StoredFile upload(MultipartFile file) throws IOException {
        return uploadOriginal(file, createAnalysisStorageContext(file == null ? null : file.getOriginalFilename()));
    }

    public StoredFile uploadOriginal(MultipartFile file, AnalysisStorageContext context) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ad check file is required.");
        }

        long fileSize = file.getSize();
        if (fileSize <= 0 || fileSize > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ad check file size must be 1 byte to 20MB.");
        }

        String originalFileName = resolveOriginalFileName(file);
        String contentType = resolveContentType(file.getContentType(), originalFileName);
        String objectKey = createArtifactObjectKey(
                context,
                "original/" + sanitizeBaseName(removeExtension(originalFileName)) + "." + EXTENSION_BY_CONTENT_TYPE.get(contentType)
        );
        String objectContentType = createBrowserContentType(contentType);

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .contentType(objectContentType)
                        .contentLength(fileSize)
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), fileSize)
        );

        return StoredFile.builder()
                .objectKey(objectKey)
                .viewUrl(createViewUrl(objectKey, contentType))
                .contentType(contentType)
                .fileSize(fileSize)
                .build();
    }

    public StoredFile uploadText(AnalysisStorageContext context, String relativePath, String text) {
        byte[] bytes = (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
        return uploadBytes(context, relativePath, bytes, "text/plain");
    }

    public StoredFile uploadJson(AnalysisStorageContext context, String relativePath, String json) {
        byte[] bytes = (json == null ? "{}" : json).getBytes(StandardCharsets.UTF_8);
        return uploadBytes(context, relativePath, bytes, "application/json");
    }

    public StoredFile uploadBytes(
            AnalysisStorageContext context,
            String relativePath,
            byte[] bytes,
            String contentType
    ) {
        String objectKey = createArtifactObjectKey(context, relativePath);
        String normalizedContentType = normalizeContentType(contentType);
        String objectContentType = createBrowserContentType(
                normalizedContentType != null ? normalizedContentType : "application/octet-stream"
        );
        byte[] payload = bytes == null ? new byte[0] : bytes;

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .contentType(objectContentType)
                        .contentLength((long) payload.length)
                        .build(),
                RequestBody.fromBytes(payload)
        );

        return StoredFile.builder()
                .objectKey(objectKey)
                .viewUrl(createViewUrl(objectKey, objectContentType))
                .contentType(objectContentType)
                .fileSize((long) payload.length)
                .build();
    }

    public String createArtifactObjectKey(AnalysisStorageContext context, String relativePath) {
        if (context == null || context.getBasePrefix() == null || context.getBasePrefix().isBlank()) {
            throw new IllegalArgumentException("analysis storage context is required.");
        }
        return context.getBasePrefix() + "/" + sanitizeRelativePath(relativePath);
    }

    public String createViewUrl(String objectKey) {
        return createViewUrl(objectKey, null);
    }

    public String createViewUrl(String objectKey, String contentType) {
        GetObjectRequest.Builder getObjectRequestBuilder = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey);
        String browserContentType = createBrowserContentType(resolveViewContentType(contentType, objectKey));
        if (browserContentType != null) {
            getObjectRequestBuilder.responseContentType(browserContentType);
        }
        GetObjectRequest getObjectRequest = getObjectRequestBuilder.build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(VIEW_EXPIRES_IN_SECONDS))
                .getObjectRequest(getObjectRequest)
                .build();
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    private String resolveViewContentType(String contentType, String objectKey) {
        String normalizedContentType = normalizeContentType(contentType);
        if (normalizedContentType != null) {
            return normalizedContentType;
        }

        String extension = extensionOf(objectKey);
        return CONTENT_TYPE_BY_EXTENSION.get(extension);
    }

    private String createBrowserContentType(String contentType) {
        String normalizedContentType = normalizeContentType(contentType);
        if ("text/plain".equals(normalizedContentType)) {
            return "text/plain; charset=UTF-8";
        }
        return normalizedContentType;
    }

    public boolean isAdCheckFileKey(String objectKey) {
        return objectKey != null && objectKey.startsWith(AD_CHECK_FILE_PREFIX + "/");
    }

    private String resolveContentType(String contentType, String fileName) {
        String normalizedContentType = normalizeContentType(contentType);
        if (EXTENSION_BY_CONTENT_TYPE.containsKey(normalizedContentType)) {
            return normalizedContentType;
        }

        String extension = extensionOf(fileName);
        String inferredContentType = CONTENT_TYPE_BY_EXTENSION.get(extension);
        if (inferredContentType != null) {
            return inferredContentType;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported ad check file content type.");
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    private String resolveOriginalFileName(MultipartFile file) {
        return resolveOriginalFileName(file.getOriginalFilename());
    }

    private String resolveOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "upload";
        }

        String normalized = originalFileName.replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        return slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
    }

    private String sanitizeBaseName(String value) {
        String sanitized = String.valueOf(value).trim().replaceAll("[^A-Za-z0-9._-]", "_");
        sanitized = sanitized.replaceAll("_+", "_").replaceAll("^[_ .-]+|[_ .-]+$", "");
        return sanitized.isBlank() ? "upload" : sanitized;
    }

    private String sanitizeRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return "artifact";
        }

        String normalized = relativePath.replace('\\', '/');
        String[] parts = normalized.split("/");
        StringBuilder sanitized = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank() || ".".equals(part) || "..".equals(part)) {
                continue;
            }
            if (sanitized.length() > 0) {
                sanitized.append('/');
            }
            sanitized.append(sanitizeBaseName(part));
        }

        if (sanitized.length() == 0) {
            return "artifact";
        }
        return sanitized.toString();
    }

    private String removeExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0) {
            return fileName;
        }
        return fileName.substring(0, dotIndex);
    }

    private String extensionOf(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    @Getter
    @Builder
    public static class StoredFile {
        private String objectKey;
        private String viewUrl;
        private String contentType;
        private Long fileSize;
    }

    @Getter
    @Builder
    public static class AnalysisStorageContext {
        private String workId;
        private String basePrefix;
    }
}
