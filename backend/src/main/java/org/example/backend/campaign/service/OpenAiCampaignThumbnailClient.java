package org.example.backend.campaign.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.backend.campaign.model.Campaign;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.Proxy;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 캠페인 썸네일을 OpenAI Image API로 생성. 16:9 비율(1536x1024).
 * 사용자가 직접 업로드하지 않은 캠페인의 썸네일을 자동 채움.
 */
@Service
public class OpenAiCampaignThumbnailClient {

    private static final String IMAGE_GENERATION_PATH = "/v1/images/generations";
    private static final String IMAGE_SIZE = "1536x1024";   // 16:9 디렉토리 카드 비율
    private static final String OUTPUT_FORMAT = "png";

    private final RestClient restClient;

    @Value("${openai.image.api-key:}")
    private String apiKey;

    @Value("${openai.image.model:gpt-image-1.5}")
    private String model;

    @Value("${openai.image.quality:low}")
    private String quality;

    public OpenAiCampaignThumbnailClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000);
        requestFactory.setReadTimeout(120000);
        requestFactory.setProxy(Proxy.NO_PROXY);
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .requestFactory(requestFactory)
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 캠페인 정보로부터 prompt 작성 → OpenAI 호출 → PNG bytes 반환.
     * 키 미설정 시 503 throw — 호출 측에서 swallow 권장.
     */
    public byte[] generateThumbnail(Campaign campaign) {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "OpenAI image API key is not configured.");
        }
        String prompt = buildPrompt(campaign);
        try {
            OpenAiImageResponse response = restClient.post()
                    .uri(IMAGE_GENERATION_PATH)
                    .headers(h -> h.setBearerAuth(apiKey.trim()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", model,
                            "prompt", prompt,
                            "n", 1,
                            "size", IMAGE_SIZE,
                            "quality", quality,
                            "output_format", OUTPUT_FORMAT
                    ))
                    .retrieve()
                    .body(OpenAiImageResponse.class);
            String b64 = resolveBase64Image(response);
            return Base64.getMimeDecoder().decode(b64);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI image response was invalid.");
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            String snippet = (body == null || body.isBlank()) ? "OpenAI image request failed."
                    : (body.length() > 600 ? body.substring(0, 600) : body);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, snippet);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI image request failed.");
        }
    }

    private String buildPrompt(Campaign c) {
        StringBuilder sb = new StringBuilder();
        sb.append("Create a clean, modern marketing campaign hero thumbnail in 16:9 ratio. ");
        sb.append("Style: editorial, soft gradients, premium SaaS aesthetic, abstract illustration. ");
        sb.append("Campaign name: \"").append(escape(c.getName())).append("\". ");
        if (c.getPurpose() != null && !c.getPurpose().isBlank()) {
            sb.append("Purpose: ").append(escape(truncate(c.getPurpose(), 200))).append(". ");
        }
        if (c.getPrimaryGoal() != null && !c.getPrimaryGoal().isBlank()) {
            sb.append("Primary goal: ").append(escape(c.getPrimaryGoal())).append(". ");
        }
        if (c.getTags() != null && !c.getTags().isEmpty()) {
            sb.append("Tags: ").append(String.join(", ", c.getTags())).append(". ");
        }
        sb.append("Do not include text, logos, watermarks, or readable letters. ");
        sb.append("Use a color palette tinted toward ").append(c.getColor() == null ? "#9D85FF" : c.getColor()).append(".");
        return sb.toString();
    }

    private static String escape(String v) {
        if (v == null) return "";
        return v.replace("\"", "'").replace("\n", " ").trim();
    }

    private static String truncate(String v, int max) {
        if (v == null) return "";
        return v.length() > max ? v.substring(0, max) : v;
    }

    private String resolveBase64Image(OpenAiImageResponse response) {
        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI image response did not contain image data.");
        }
        String b64 = response.data().get(0).b64Json();
        if (b64 == null || b64.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI image response did not contain image data.");
        }
        return b64;
    }

    private record OpenAiImageResponse(List<OpenAiImageData> data) {}
    private record OpenAiImageData(@JsonProperty("b64_json") String b64Json) {}
}
