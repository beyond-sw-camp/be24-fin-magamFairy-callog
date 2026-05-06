package org.example.backend.userInfo.userProfile.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.user.model.User;
import org.example.backend.userInfo.userProfile.model.UserProfileDto;
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

@Service
@RequiredArgsConstructor
public class ProfileImageStorageService {
    private static final String PROFILE_IMAGE_PREFIX = "profileimg/";
    private static final String APPLIED_IMAGE_DIRECTORY = "applied";
    private static final String GENERATED_IMAGE_DIRECTORY = "generated";
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

    public UserProfileDto.ProfileImageUploadUrlRes createUploadUrl(
            User user,
            UserProfileDto.ProfileImageUploadUrlReq dto
    ) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required.");
        }

        String contentType = normalizeContentType(dto.contentType());
        resolveFileSize(dto.fileSize());
        String objectKey = createAppliedProfileImageKey(user, contentType);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(UPLOAD_EXPIRES_IN_SECONDS))
                .putObjectRequest(putObjectRequest)
                .build();
        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        return UserProfileDto.ProfileImageUploadUrlRes.builder()
                .objectKey(objectKey)
                .uploadUrl(presignedRequest.url().toString())
                .expiresInSeconds(UPLOAD_EXPIRES_IN_SECONDS)
                .contentType(contentType)
                .build();
    }

    public void validateUploadedObject(String objectKey) {
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
            normalizeContentType(response.contentType());
            resolveFileSize(response.contentLength());
        } catch (S3Exception exception) {
            if (exception.statusCode() == HttpStatus.NOT_FOUND.value()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "profile image was not uploaded.");
            }

            throw exception;
        }
    }

    public String createViewUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(VIEW_EXPIRES_IN_SECONDS))
                .getObjectRequest(getObjectRequest)
                .build();
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

        return presignedRequest.url().toString();
    }

    public void deleteObject(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build());
    }

    public void uploadProfileImageObject(String objectKey, byte[] imageBytes, String contentType) {
        String normalizedContentType = normalizeContentType(contentType);

        if (objectKey == null || objectKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "objectKey is required.");
        }

        if (imageBytes == null || imageBytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "generated profile image is empty.");
        }

        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .contentType(normalizedContentType)
                        .build(),
                RequestBody.fromBytes(imageBytes));
    }

    public boolean isProfileImageKeyForUser(User user, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return false;
        }

        String userKeyPrefix = userKeyPrefix(user);
        String legacyPrefix = userKeyPrefix + ".";
        String appliedPrefix = userKeyPrefix + "/" + APPLIED_IMAGE_DIRECTORY + "/";
        String generatedPrefix = userKeyPrefix + "/" + GENERATED_IMAGE_DIRECTORY + "/";

        return (objectKey.startsWith(legacyPrefix)
                || objectKey.startsWith(appliedPrefix)
                || objectKey.startsWith(generatedPrefix))
                && EXTENSION_BY_CONTENT_TYPE.containsValue(extensionOf(objectKey));
    }

    public String createGeneratedProfileImageKey(User user) {
        return userKeyPrefix(user)
                + "/"
                + GENERATED_IMAGE_DIRECTORY
                + "/"
                + uniqueFileName("png");
    }

    private String createAppliedProfileImageKey(User user, String contentType) {
        return userKeyPrefix(user)
                + "/"
                + APPLIED_IMAGE_DIRECTORY
                + "/"
                + uniqueFileName(EXTENSION_BY_CONTENT_TYPE.get(contentType));
    }

    private String userKeyPrefix(User user) {
        return PROFILE_IMAGE_PREFIX
                + user.getIdx()
                + "_"
                + sanitizeLoginId(user.getId());
    }

    private String uniqueFileName(String extension) {
        return OBJECT_KEY_TIMESTAMP_FORMATTER.format(Instant.now())
                + "_"
                + UUID.randomUUID()
                + "."
                + extension;
    }

    private String normalizeContentType(String contentType) {
        String normalizedValue = String.valueOf(contentType).trim().toLowerCase();

        if (!EXTENSION_BY_CONTENT_TYPE.containsKey(normalizedValue)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported profile image content type.");
        }

        return normalizedValue;
    }

    private long resolveFileSize(Long fileSize) {
        long normalizedSize = Objects.requireNonNullElse(fileSize, 0L);

        if (normalizedSize <= 0 || normalizedSize > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "profile image size must be 1 byte to 5MB.");
        }

        return normalizedSize;
    }

    private String sanitizeLoginId(String loginId) {
        String sanitizedValue = String.valueOf(loginId).trim().replaceAll("[^A-Za-z0-9._-]", "_");

        return sanitizedValue.isBlank() ? "user" : sanitizedValue;
    }

    private String extensionOf(String objectKey) {
        int dotIndex = objectKey.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == objectKey.length() - 1) {
            return "";
        }

        return objectKey.substring(dotIndex + 1).toLowerCase();
    }
}
