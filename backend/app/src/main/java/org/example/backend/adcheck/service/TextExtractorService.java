package org.example.backend.adcheck.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
public class TextExtractorService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".bmp", ".jpeg", ".jpg", ".png", ".tif", ".tiff", ".webp"
    );

    private final ObjectMapper objectMapper;
    private final RestClient layoutRestClient;
    private final RestClient cropRestClient;
    private final RestClient ocrRestClient;

    @Value("${custom.layout.url:http://localhost:8001/v1/layout/analyze}")
    private String layoutUrl;

    @Value("${custom.ocr.url}")
    private String ocrUrl;

    public TextExtractorService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory longRunningFactory = new SimpleClientHttpRequestFactory();
        longRunningFactory.setConnectTimeout(5000);
        longRunningFactory.setReadTimeout(120000);
        longRunningFactory.setProxy(Proxy.NO_PROXY);

        SimpleClientHttpRequestFactory cropFactory = new SimpleClientHttpRequestFactory();
        cropFactory.setConnectTimeout(5000);
        cropFactory.setReadTimeout(30000);
        cropFactory.setProxy(Proxy.NO_PROXY);

        this.layoutRestClient = RestClient.builder()
                .requestFactory(longRunningFactory)
                .build();
        this.ocrRestClient = RestClient.builder()
                .requestFactory(longRunningFactory)
                .build();
        this.cropRestClient = RestClient.builder()
                .requestFactory(cropFactory)
                .build();
    }

    public String extract(MultipartFile file) throws IOException {
        return extractWithTiming(file).text();
    }

    public ExtractResult extractWithTiming(MultipartFile file) throws IOException {
        long totalStartedAt = System.nanoTime();
        String contentType = file.getContentType() != null ? file.getContentType() : "";
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        byte[] bytes = file.getBytes();

        if (isPlainText(contentType, filename)) {
            long textStartedAt = System.nanoTime();
            String text = new String(bytes, StandardCharsets.UTF_8);
            return new ExtractResult(
                    text,
                    "plain_text",
                    elapsedMillis(textStartedAt),
                    0L,
                    0L,
                    elapsedMillis(totalStartedAt)
            );
        }

        if (isPdf(contentType, filename)) {
            long pdfStartedAt = System.nanoTime();
            String text = extractFromPdf(bytes);
            long pdfTextMillis = elapsedMillis(pdfStartedAt);
            if (StringUtils.hasText(text)) {
                return new ExtractResult(
                        text,
                        "pdf_embedded_text",
                        pdfTextMillis,
                        0L,
                        0L,
                        elapsedMillis(totalStartedAt)
                );
            }

            LayoutOcrResult layoutOcr = extractViaLayoutThenOcr(bytes, filename, contentType);
            return new ExtractResult(
                    layoutOcr.text(),
                    layoutOcr.usedFallback() ? "pdf_ocr_fallback" : "pdf_layout_ocr",
                    pdfTextMillis,
                    layoutOcr.layoutMillis(),
                    layoutOcr.ocrMillis(),
                    elapsedMillis(totalStartedAt)
            );
        }

        if (isImage(contentType, filename)) {
            LayoutOcrResult layoutOcr = extractViaLayoutThenOcr(bytes, filename, contentType);
            return new ExtractResult(
                    layoutOcr.text(),
                    layoutOcr.usedFallback() ? "image_ocr_fallback" : "image_layout_ocr",
                    0L,
                    layoutOcr.layoutMillis(),
                    layoutOcr.ocrMillis(),
                    elapsedMillis(totalStartedAt)
            );
        }

        throw new IllegalArgumentException("Unsupported file type: " + contentType);
    }

    private String extractFromPdf(byte[] bytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private LayoutOcrResult extractViaLayoutThenOcr(byte[] bytes, String filename, String contentType) {
        long layoutStartedAt = System.nanoTime();
        long layoutMillis = 0L;
        long ocrStartedAt = 0L;

        try {
            JsonNode layoutResult = requestLayout(bytes, filename, contentType);
            layoutMillis = elapsedMillis(layoutStartedAt);

            List<OcrTarget> targets = extractOcrTargets(layoutResult);
            if (targets.isEmpty()) {
                throw new RuntimeException("Layout OCR targets are empty.");
            }

            ocrStartedAt = System.nanoTime();
            List<String> texts = new ArrayList<>();
            for (OcrTarget target : targets) {
                byte[] cropBytes = downloadCrop(target);
                String targetText = extractViaOcr(cropBytes, targetFilename(target), MediaType.IMAGE_PNG_VALUE);
                if (StringUtils.hasText(targetText)) {
                    texts.add(targetText.trim());
                }
            }

            String text = String.join("\n\n", texts);
            if (!StringUtils.hasText(text)) {
                throw new RuntimeException("Layout OCR text is empty.");
            }

            return new LayoutOcrResult(text, layoutMillis, elapsedMillis(ocrStartedAt), false);
        } catch (RuntimeException e) {
            layoutMillis = layoutMillis == 0L ? elapsedMillis(layoutStartedAt) : layoutMillis;
            long failedTargetOcrMillis = ocrStartedAt == 0L ? 0L : elapsedMillis(ocrStartedAt);
            log.warn("Layout OCR flow failed. fallback to full-file OCR. fileName={}, layoutUrl={}",
                    filename, layoutUrl, e);

            long fallbackOcrStartedAt = System.nanoTime();
            String text = extractViaOcr(bytes, filename, contentType);
            return new LayoutOcrResult(
                    text,
                    layoutMillis,
                    failedTargetOcrMillis + elapsedMillis(fallbackOcrStartedAt),
                    true
            );
        }
    }

    private JsonNode requestLayout(byte[] bytes, String filename, String contentType) {
        log.info("Calling layout-parser service. url={}, fileName={}, contentType={}, size={}",
                layoutUrl, filename, contentType, bytes.length);

        MultiValueMap<String, Object> body = createMultipartBody(bytes, filename, contentType);
        body.add("document_id", normalizeDocumentId(filename));
        body.add("include_image_targets", "false");

        try {
            byte[] rawBytes = layoutRestClient.post()
                    .uri(layoutUrl)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(byte[].class);

            String raw = rawBytes == null ? "" : new String(rawBytes, StandardCharsets.UTF_8);
            JsonNode result = objectMapper.readTree(raw);
            log.info("Layout-parser service response received. fileName={}, ocrTargetCount={}",
                    filename, extractOcrTargets(result).size());
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Layout service response cannot be parsed.", e);
        } catch (RestClientResponseException e) {
            log.error("Layout service returned error status. url={}, status={}, body={}",
                    layoutUrl, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Layout service returned HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            log.error("Layout service request failed. url={}, fileName={}", layoutUrl, filename, e);
            throw new RuntimeException("Layout service is unavailable.", e);
        }
    }

    private List<OcrTarget> extractOcrTargets(JsonNode layoutResult) {
        JsonNode targetsNode = layoutResult.path("ocr_targets");
        if (!targetsNode.isArray()) {
            targetsNode = layoutResult.path("downstream_targets").path("ocr_targets");
        }
        if (!targetsNode.isArray()) {
            return List.of();
        }

        List<OcrTarget> targets = new ArrayList<>();
        for (JsonNode target : targetsNode) {
            String cropUrl = text(target, "crop_url", "image_url");
            if (!StringUtils.hasText(cropUrl)) {
                cropUrl = text(target.path("input"), "image_url", "crop_url");
            }
            if (!StringUtils.hasText(cropUrl)) {
                cropUrl = text(target.path("metadata"), "image_url", "crop_url");
            }
            if (!StringUtils.hasText(cropUrl)) {
                continue;
            }

            String targetId = text(target, "target_id", "id");
            targets.add(new OcrTarget(
                    StringUtils.hasText(targetId) ? targetId : "ocr-" + (targets.size() + 1),
                    intValue(target, "page", 0),
                    intValue(target, "reading_order", intValue(target.path("metadata"), "reading_order", 0)),
                    cropUrl
            ));
        }

        targets.sort(Comparator
                .comparingInt(OcrTarget::page)
                .thenComparingInt(OcrTarget::readingOrder)
                .thenComparing(OcrTarget::targetId));
        return targets;
    }

    private byte[] downloadCrop(OcrTarget target) {
        try {
            byte[] bytes = cropRestClient.get()
                    .uri(target.cropUrl())
                    .retrieve()
                    .body(byte[].class);
            if (bytes == null || bytes.length == 0) {
                throw new RuntimeException("Layout crop is empty: " + target.cropUrl());
            }
            return bytes;
        } catch (RestClientException e) {
            throw new RuntimeException("Layout crop cannot be downloaded: " + target.cropUrl(), e);
        }
    }

    private String extractViaOcr(byte[] bytes, String filename, String contentType) {
        log.info("Calling OCR service. url={}, fileName={}, contentType={}, size={}",
                ocrUrl, filename, contentType, bytes.length);

        MultiValueMap<String, Object> body = createMultipartBody(bytes, filename, contentType);

        try {
            OcrResponse response = ocrRestClient.post()
                    .uri(ocrUrl)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(OcrResponse.class);

            if (response == null || !StringUtils.hasText(response.getText())) {
                log.warn("OCR service returned empty text. url={}, fileName={}, pageCount={}",
                        ocrUrl, filename, response == null ? null : response.getPageCount());
                throw new RuntimeException("OCR result is empty.");
            }

            log.info("OCR service response received. fileName={}, textLength={}, pageCount={}",
                    filename, response.getText().length(), response.getPageCount());
            return response.getText();
        } catch (RestClientResponseException e) {
            log.error("OCR service returned error status. url={}, status={}, body={}",
                    ocrUrl, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("OCR service returned HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            log.error("OCR service request failed. url={}, fileName={}", ocrUrl, filename, e);
            throw new RuntimeException("OCR service is unavailable.", e);
        }
    }

    private MultiValueMap<String, Object> createMultipartBody(byte[] bytes, String filename, String contentType) {
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(resolveMediaType(contentType));
        fileHeaders.setContentDisposition(ContentDisposition.formData()
                .name("file")
                .filename(filename, StandardCharsets.UTF_8)
                .build());

        HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(
                new NamedByteArrayResource(bytes, filename),
                fileHeaders
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", filePart);
        return body;
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

    private boolean isPlainText(String contentType, String filename) {
        return contentType.startsWith("text/plain") || extensionOf(filename).equals(".txt");
    }

    private boolean isPdf(String contentType, String filename) {
        return contentType.equals(MediaType.APPLICATION_PDF_VALUE) || extensionOf(filename).equals(".pdf");
    }

    private boolean isImage(String contentType, String filename) {
        return contentType.startsWith("image/") || IMAGE_EXTENSIONS.contains(extensionOf(filename));
    }

    private String extensionOf(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return filename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private String normalizeDocumentId(String filename) {
        String baseName = filename;
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = baseName.substring(0, dotIndex);
        }
        String normalized = baseName.replaceAll("[^A-Za-z0-9_-]+", "-");
        return StringUtils.hasText(normalized) ? normalized : "upload";
    }

    private String targetFilename(OcrTarget target) {
        return target.targetId().replaceAll("[^A-Za-z0-9_-]+", "-") + ".png";
    }

    private String text(JsonNode node, String... names) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        for (String name : names) {
            JsonNode value = node.path(name);
            if (value.isTextual() && StringUtils.hasText(value.asText())) {
                return value.asText().trim();
            }
        }
        return "";
    }

    private int intValue(JsonNode node, String name, int defaultValue) {
        JsonNode value = node == null ? null : node.path(name);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return defaultValue;
        }
        if (value.isInt() || value.isLong()) {
            return value.asInt(defaultValue);
        }
        if (value.isTextual()) {
            try {
                return Integer.parseInt(value.asText().trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }

    public record ExtractResult(
            String text,
            String extractionMode,
            Long textExtractionMillis,
            Long layoutMillis,
            Long ocrMillis,
            Long totalMillis
    ) {
    }

    private record LayoutOcrResult(
            String text,
            Long layoutMillis,
            Long ocrMillis,
            boolean usedFallback
    ) {
    }

    private record OcrTarget(
            String targetId,
            int page,
            int readingOrder,
            String cropUrl
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OcrResponse {
        private String text;
        private Integer pageCount;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Integer getPageCount() {
            return pageCount;
        }

        public void setPageCount(Integer pageCount) {
            this.pageCount = pageCount;
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
