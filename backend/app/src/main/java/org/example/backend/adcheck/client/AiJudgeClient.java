package org.example.backend.adcheck.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.adcheck.model.AdCheckDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Slf4j
public class AiJudgeClient {

    private static final String CHECK_PATH = "/multi/aijudge/check";
    private static final String CHECK_FILE_PATH = "/multi/aijudge/check/file";

    private final RestClient aiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${ai-judge.base-url:http://localhost:8081}")
    private String aiJudgeBaseUrl;

    public AiJudgeClient(@Qualifier("aiRestClient") RestClient aiRestClient, ObjectMapper objectMapper) {
        this.aiRestClient = aiRestClient;
        this.objectMapper = objectMapper;
    }

    public AdCheckDto.Res check(String copy) {
        String url = normalizeBaseUrl(aiJudgeBaseUrl) + CHECK_PATH;
        AdCheckDto.Req req = AdCheckDto.Req.builder()
                .copy(copy)
                .build();

        try {
            log.info("Calling ai-judge service. url={}, copyLength={}", url, copy == null ? 0 : copy.length());
            AdCheckDto.Res response = aiRestClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(AdCheckDto.Res.class);

            if (response == null) {
                throw new RuntimeException("ai-judge returned an empty response.");
            }
            return response;
        } catch (ResourceAccessException e) {
            log.error("ai-judge service connection/read failed. url={}", url, e);
            throw new RuntimeException("ai-judge service is unreachable or timed out.", e);
        } catch (RestClientResponseException e) {
            log.error("ai-judge service returned error status. url={}, status={}, body={}",
                    url, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException(
                    "ai-judge call failed. HTTP " + e.getStatusCode() + " response: " + e.getResponseBodyAsString(),
                    e
            );
        } catch (RestClientException e) {
            log.error("ai-judge service call failed. url={}", url, e);
            throw new RuntimeException("ai-judge service call failed.", e);
        }
    }

    public AdCheckDto.FileCheckRes checkFile(MultipartFile file) {
        return checkFile(file, Map.of());
    }

    public AdCheckDto.FileCheckRes checkFile(MultipartFile file, Map<String, Object> context) {
        String url = normalizeBaseUrl(aiJudgeBaseUrl) + CHECK_FILE_PATH;

        try {
            log.info("Calling ai-judge file service. url={}, fileName={}, size={}",
                    url, file == null ? null : file.getOriginalFilename(), file == null ? 0 : file.getSize());

            AdCheckDto.FileCheckRes response = aiRestClient.post()
                    .uri(url)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(createMultipartBody(file, context))
                    .retrieve()
                    .body(AdCheckDto.FileCheckRes.class);

            if (response == null) {
                throw new RuntimeException("ai-judge returned an empty file check response.");
            }
            return response;
        } catch (ResourceAccessException e) {
            log.error("ai-judge file service connection/read failed. url={}", url, e);
            throw new RuntimeException("ai-judge file service is unreachable or timed out.", e);
        } catch (RestClientResponseException e) {
            AdCheckDto.FileCheckRes partialResponse = parseFileCheckResponse(e.getResponseBodyAsString());
            log.error("ai-judge file service returned error status. url={}, status={}, body={}",
                    url, e.getStatusCode(), e.getResponseBodyAsString(), e);
            if (partialResponse != null) {
                throw new FileCheckRemoteException(
                        "ai-judge file check failed. HTTP " + e.getStatusCode(),
                        partialResponse,
                        e
                );
            }
            throw new RuntimeException(
                    "ai-judge file check failed. HTTP " + e.getStatusCode() + " response: " + e.getResponseBodyAsString(),
                    e
            );
        } catch (RestClientException e) {
            log.error("ai-judge file service call failed. url={}", url, e);
            throw new RuntimeException("ai-judge file service call failed.", e);
        }
    }

    private MultiValueMap<String, Object> createMultipartBody(MultipartFile file, Map<String, Object> context) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("ad check file is required.");
        }

        try {
            String filename = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "upload";
            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(resolveMediaType(file.getContentType()));
            fileHeaders.setContentDisposition(ContentDisposition.formData()
                    .name("file")
                    .filename(filename, StandardCharsets.UTF_8)
                    .build());

            HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(
                    new NamedByteArrayResource(file.getBytes(), filename),
                    fileHeaders
            );

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", filePart);
            if (context != null && !context.isEmpty()) {
                HttpHeaders contextHeaders = new HttpHeaders();
                contextHeaders.setContentType(MediaType.APPLICATION_JSON);
                body.add("context", new HttpEntity<>(objectMapper.writeValueAsString(context), contextHeaders));
            }
            return body;
        } catch (IOException e) {
            throw new RuntimeException("ad check file cannot be read.", e);
        }
    }

    private MediaType resolveMediaType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private AdCheckDto.FileCheckRes parseFileCheckResponse(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }

        try {
            return objectMapper.readValue(raw, AdCheckDto.FileCheckRes.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8082";
        }
        return baseUrl.replaceAll("/+$", "");
    }

    public static class FileCheckRemoteException extends RuntimeException {
        private final AdCheckDto.FileCheckRes response;

        public FileCheckRemoteException(String message, AdCheckDto.FileCheckRes response, Throwable cause) {
            super(message, cause);
            this.response = response;
        }

        public AdCheckDto.FileCheckRes getResponse() {
            return response;
        }
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
