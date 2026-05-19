package org.example.backend.adcheck.client;

import lombok.extern.slf4j.Slf4j;
import org.example.backend.adcheck.model.AdCheckDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Slf4j
public class AiJudgeClient {

    private static final String CHECK_PATH = "/multi/aijudge/check";

    private final RestClient aiRestClient;

    @Value("${ai-judge.base-url:http://localhost:8081}")
    private String aiJudgeBaseUrl;

    public AiJudgeClient(@Qualifier("aiRestClient") RestClient aiRestClient) {
        this.aiRestClient = aiRestClient;
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

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8081";
        }
        return baseUrl.replaceAll("/+$", "");
    }
}
