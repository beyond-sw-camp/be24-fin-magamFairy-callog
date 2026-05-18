package org.example.backend.userInfo.service;

import com.fasterxml.jackson.annotation.JsonProperty;
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

@Service
public class OpenAiProfileImageClient {
    private static final String IMAGE_GENERATION_PATH = "/v1/images/generations";
    private static final String IMAGE_SIZE = "1024x1024";
    private static final String OUTPUT_FORMAT = "png";

    private final RestClient restClient;

    @Value("${openai.image.api-key:}")
    private String apiKey;

    @Value("${openai.image.model:gpt-image-1.5}")
    private String model;

    @Value("${openai.image.quality:low}")
    private String quality;

    public OpenAiProfileImageClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000);
        requestFactory.setReadTimeout(120000);
        requestFactory.setProxy(Proxy.NO_PROXY);
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .requestFactory(requestFactory)
                .build();
    }

    public String getModel() {
        return model;
    }

    public byte[] generateProfileImage(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OpenAI image API key is not configured.");
        }

        try {
            OpenAiImageResponse response = restClient.post()
                    .uri(IMAGE_GENERATION_PATH)
                    .headers(headers -> headers.setBearerAuth(apiKey.trim()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", model,
                            "prompt", createProfilePrompt(prompt),
                            "n", 1,
                            "size", IMAGE_SIZE,
                            "quality", quality,
                            "output_format", OUTPUT_FORMAT
                    ))
                    .retrieve()
                    .body(OpenAiImageResponse.class);

            String encodedImage = resolveBase64Image(response);

            return Base64.getMimeDecoder().decode(encodedImage);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI image response was invalid.");
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, resolveOpenAiError(exception));
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI image request failed.");
        }
    }

    private String createProfilePrompt(String prompt) {
        return "Create a square 1024x1024 professional profile image based on: "
                + prompt
                + ". Do not include text, logos, watermarks, or readable letters.";
    }

    private String resolveBase64Image(OpenAiImageResponse response) {
        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI image response did not contain image data.");
        }

        String encodedImage = response.data().get(0).b64Json();

        if (encodedImage == null || encodedImage.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI image response did not contain image data.");
        }

        return encodedImage;
    }

    private String resolveOpenAiError(RestClientResponseException exception) {
        String body = exception.getResponseBodyAsString();

        if (body == null || body.isBlank()) {
            return "OpenAI image request failed.";
        }

        return body.length() > 1000 ? body.substring(0, 1000) : body;
    }

    private record OpenAiImageResponse(List<OpenAiImageData> data) {
    }

    private record OpenAiImageData(@JsonProperty("b64_json") String b64Json) {
    }
}
