package org.example.backend.adcheck.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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

@Component
@Slf4j
public class AiJudgeClient {

    private static final String CHECK_PATH = "/aijudge/check";
    private static final String CHECK_FILE_PATH = "/aijudge/check/file";
    private static final String ANALYSIS_DETAIL_PATH = "/aijudge/analyses/{analysisJobId}";
    private static final String DETAIL_CIRCUIT_BREAKER = "aiJudgeDetail";

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
        return checkFile(file, null);
    }

    public AdCheckDto.FileCheckRes checkFile(MultipartFile file, String analysisJobId) {
        String url = normalizeBaseUrl(aiJudgeBaseUrl) + CHECK_FILE_PATH;

        try {
            log.info("Calling ai-judge file service. url={}, analysisJobId={}, fileName={}, size={}",
                    url, analysisJobId, file == null ? null : file.getOriginalFilename(), file == null ? 0 : file.getSize());

            AdCheckDto.FileCheckRes response = aiRestClient.post()
                    .uri(url + "?analysisJobId={analysisJobId}", analysisJobId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(createMultipartBody(file))
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

    @CircuitBreaker(name = DETAIL_CIRCUIT_BREAKER, fallbackMethod = "analysisDetailFallback")
    public AdCheckDto.FileCheckRes getAnalysisDetail(String analysisJobId) {
        if (!StringUtils.hasText(analysisJobId)) {
            throw new DetailUnavailableException("analysis job id is required.");
        }

        String url = normalizeBaseUrl(aiJudgeBaseUrl) + ANALYSIS_DETAIL_PATH;
        try {
            log.info("Calling ai-judge analysis detail service. analysisJobId={}", analysisJobId);
            AdCheckDto.FileCheckRes response = aiRestClient.get()
                    .uri(url, analysisJobId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(AdCheckDto.FileCheckRes.class);

            if (response == null) {
                throw new DetailUnavailableException("ai-judge returned an empty analysis detail response.");
            }
            return response;
        } catch (DetailUnavailableException e) {
            throw e;
        } catch (ResourceAccessException e) {
            log.warn("ai-judge analysis detail connection/read failed. analysisJobId={}", analysisJobId, e);
            throw new DetailUnavailableException("ai-judge analysis detail is unreachable or timed out.", e);
        } catch (RestClientResponseException e) {
            log.warn("ai-judge analysis detail returned error status. analysisJobId={}, status={}",
                    analysisJobId, e.getStatusCode(), e);
            throw new DetailUnavailableException("ai-judge analysis detail returned HTTP " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.warn("ai-judge analysis detail call failed. analysisJobId={}", analysisJobId, e);
            throw new DetailUnavailableException("ai-judge analysis detail call failed.", e);
        }
    }

    public AdCheckDto.FileCheckRes analysisDetailFallback(String analysisJobId, Throwable cause) {
        if (cause instanceof DetailUnavailableException detailUnavailableException) {
            throw detailUnavailableException;
        }
        throw new DetailUnavailableException("ai-judge analysis detail is temporarily unavailable.", cause);
    }

    private MultiValueMap<String, Object> createMultipartBody(MultipartFile file) {
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
            return "http://localhost:8081";
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

    public static class DetailUnavailableException extends RuntimeException {
        public DetailUnavailableException(String message) {
            super(message);
        }

        public DetailUnavailableException(String message, Throwable cause) {
            super(message, cause);
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
    }//
}
