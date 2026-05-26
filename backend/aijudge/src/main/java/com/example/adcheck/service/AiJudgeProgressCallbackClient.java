package com.example.adcheck.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
@Slf4j
public class AiJudgeProgressCallbackClient {
    private static final String CONTEXT_JOB_ID = "adCheckJobId";
    private static final String CONTEXT_PROGRESS_TOKEN = "adCheckProgressToken";
    private static final String CONTEXT_PROGRESS_CALLBACK_URL = "adCheckProgressCallbackUrl";

    private final RestClient restClient;

    public AiJudgeProgressCallbackClient(@Qualifier("aiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public void notify(Map<String, Object> context, String step) {
        String callbackUrl = text(context, CONTEXT_PROGRESS_CALLBACK_URL);
        String jobId = text(context, CONTEXT_JOB_ID);
        String token = text(context, CONTEXT_PROGRESS_TOKEN);
        if (!hasText(callbackUrl) || !hasText(jobId) || !hasText(token) || !hasText(step)) {
            return;
        }

        try {
            restClient.post()
                    .uri(callbackUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "jobId", jobId,
                            "token", token,
                            "step", step
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("AI judge progress callback failed. jobId={}, step={}, url={}", jobId, step, callbackUrl, e);
        }
    }

    private String text(Map<String, Object> context, String key) {
        if (context == null || key == null) {
            return null;
        }
        Object value = context.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
