package org.example.backend.adcheck.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class TextExtractorService {

    private static final String LAYOUT_API_KEY_HEADER = "X-API-Key";
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".bmp", ".jpeg", ".jpg", ".png", ".tif", ".tiff", ".webp"
    );

    private final ObjectMapper objectMapper;
    private final RestClient layoutRestClient;
    private final RestClient cropRestClient;
    private final RestClient ocrRestClient;
    private final String layoutServiceApiKey;

    @Value("${custom.layout.url:http://localhost:8001/v1/layout/analyze}")
    private String layoutUrl;

    @Value("${custom.ocr.url}")
    private String ocrUrl;

    @Value("${custom.ocr.async-enabled:true}")
    private boolean ocrAsyncEnabled;

    @Value("${custom.ocr.job-url:}")
    private String ocrJobUrl;

    @Value("${custom.ocr.job-poll-interval-ms:1000}")
    private long ocrJobPollIntervalMs;

    @Value("${custom.ocr.job-poll-timeout-ms:180000}")
    private long ocrJobPollTimeoutMs;

    public TextExtractorService(
            ObjectMapper objectMapper,
            @Value("${custom.layout.api-key:}") String layoutServiceApiKey
    ) {
        this.objectMapper = objectMapper;
        this.layoutServiceApiKey = StringUtils.hasText(layoutServiceApiKey) ? layoutServiceApiKey.trim() : "";

        SimpleClientHttpRequestFactory longRunningFactory = new SimpleClientHttpRequestFactory();
        longRunningFactory.setConnectTimeout(5000);
        longRunningFactory.setReadTimeout(120000);
        longRunningFactory.setProxy(Proxy.NO_PROXY);

        SimpleClientHttpRequestFactory cropFactory = new SimpleClientHttpRequestFactory();
        cropFactory.setConnectTimeout(5000);
        cropFactory.setReadTimeout(30000);
        cropFactory.setProxy(Proxy.NO_PROXY);

        this.layoutRestClient = withLayoutServiceApiKey(RestClient.builder()
                .requestFactory(longRunningFactory))
                .build();
        this.ocrRestClient = withLayoutServiceApiKey(RestClient.builder()
                .requestFactory(longRunningFactory))
                .build();
        this.cropRestClient = withLayoutServiceApiKey(RestClient.builder()
                .requestFactory(cropFactory))
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
                    elapsedMillis(totalStartedAt),
                    List.of()
            );
        }

        if (isPdf(contentType, filename)) {
            long pdfStartedAt = System.nanoTime();
            String text = extractFromPdf(bytes);
            long pdfTextMillis = elapsedMillis(pdfStartedAt);
            if (StringUtils.hasText(text)) {
                LayoutAssetsResult layoutAssets = extractLayoutAssets(bytes, filename, contentType);
                return new ExtractResult(
                        text,
                        "pdf_embedded_text",
                        pdfTextMillis,
                        layoutAssets.layoutMillis(),
                        0L,
                        elapsedMillis(totalStartedAt),
                        layoutAssets.images()
                );
            }

            LayoutOcrResult layoutOcr = extractViaLayoutThenOcr(bytes, filename, contentType);
            return new ExtractResult(
                    layoutOcr.text(),
                    layoutOcr.usedFallback() ? "pdf_ocr_fallback" : "pdf_layout_ocr",
                    pdfTextMillis,
                    layoutOcr.layoutMillis(),
                    layoutOcr.ocrMillis(),
                    elapsedMillis(totalStartedAt),
                    layoutOcr.images()
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
                    elapsedMillis(totalStartedAt),
                    layoutOcr.images()
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
        List<ExtractedImageAsset> images = List.of();
        String layoutJobId = null;

        try {
            JsonNode layoutResult = requestLayout(bytes, filename, contentType);
            layoutMillis = elapsedMillis(layoutStartedAt);
            images = extractImageAssets(layoutResult);
            layoutJobId = text(layoutResult, "job_id");

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

            return new LayoutOcrResult(text, layoutMillis, elapsedMillis(ocrStartedAt), false, images, layoutJobId);
        } catch (OcrServiceResourceException e) {
            layoutMillis = layoutMillis == 0L ? elapsedMillis(layoutStartedAt) : layoutMillis;
            log.warn("Layout OCR flow stopped because OCR service is busy or unavailable. skip full-file fallback. "
                    + "fileName={}, layoutUrl={}", filename, layoutUrl, e);
            throw e;
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
                    true,
                    images,
                    layoutJobId
            );
        }
    }

    private LayoutAssetsResult extractLayoutAssets(byte[] bytes, String filename, String contentType) {
        long layoutStartedAt = System.nanoTime();
        try {
            JsonNode layoutResult = requestLayout(bytes, filename, contentType);
            return new LayoutAssetsResult(
                    extractImageAssets(layoutResult),
                    elapsedMillis(layoutStartedAt),
                    text(layoutResult, "job_id")
            );
        } catch (RuntimeException e) {
            log.warn("Layout asset extraction failed. continue without extracted image assets. fileName={}, layoutUrl={}",
                    filename, layoutUrl, e);
            return new LayoutAssetsResult(List.of(), elapsedMillis(layoutStartedAt), null);
        }
    }

    private JsonNode requestLayout(byte[] bytes, String filename, String contentType) {
        requireLayoutServiceApiKey();
        log.info("Calling layout-parser service. url={}, fileName={}, contentType={}, size={}",
                layoutUrl, filename, contentType, bytes.length);

        MultiValueMap<String, Object> body = createMultipartBody(bytes, filename, contentType);
        body.add("document_id", normalizeDocumentId(filename));
        body.add("include_image_targets", "true");

        try {
            byte[] rawBytes = layoutRestClient.post()
                    .uri(layoutUrl)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(byte[].class);

            String raw = rawBytes == null ? "" : new String(rawBytes, StandardCharsets.UTF_8);
            JsonNode result = objectMapper.readTree(raw);
            log.info("Layout-parser service response received. fileName={}, ocrTargetCount={}, imageTargetCount={}",
                    filename, extractOcrTargets(result).size(), extractImageTargets(result).size());
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
        return downloadCrop(target.cropUrl());
    }

    private byte[] downloadCrop(String cropUrl) {
        requireLayoutServiceApiKey();
        try {
            byte[] bytes = cropRestClient.get()
                    .uri(cropUrl)
                    .retrieve()
                    .body(byte[].class);
            if (bytes == null || bytes.length == 0) {
                throw new RuntimeException("Layout crop is empty: " + cropUrl);
            }
            return bytes;
        } catch (RestClientException e) {
            throw new RuntimeException("Layout crop cannot be downloaded: " + cropUrl, e);
        }
    }

    private List<ExtractedImageAsset> extractImageAssets(JsonNode layoutResult) {
        List<ImageTarget> candidates = extractImageTargets(layoutResult);
        if (candidates.isEmpty()) {
            return List.of();
        }

        candidates.sort(Comparator
                .comparingInt(ImageTarget::page)
                .thenComparing(Comparator.comparingDouble(ImageTarget::area).reversed())
                .thenComparingInt(ImageTarget::readingOrder)
                .thenComparing(ImageTarget::targetId));

        Map<Integer, Integer> selectedByPage = new HashMap<>();
        List<ExtractedImageAsset> assets = new ArrayList<>();
        for (ImageTarget target : candidates) {
            int selectedCount = selectedByPage.getOrDefault(target.page(), 0);
            if (selectedCount >= 2) {
                continue;
            }

            try {
                byte[] bytes = downloadCrop(target.cropUrl());
                assets.add(new ExtractedImageAsset(
                        target.targetId(),
                        target.page(),
                        target.readingOrder(),
                        "image/png",
                        target.area(),
                        bytes
                ));
                selectedByPage.put(target.page(), selectedCount + 1);
            } catch (RuntimeException e) {
                log.warn("Layout image crop cannot be downloaded. skip image asset. targetId={}, cropUrl={}",
                        target.targetId(), target.cropUrl(), e);
            }
        }
        return assets;
    }

    private List<ImageTarget> extractImageTargets(JsonNode layoutResult) {
        JsonNode targetsNode = layoutResult.path("image_targets");
        if (!targetsNode.isArray()) {
            targetsNode = layoutResult.path("downstream_targets").path("image_targets");
        }
        if (!targetsNode.isArray()) {
            return List.of();
        }

        List<ImageTarget> targets = new ArrayList<>();
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
            targets.add(new ImageTarget(
                    StringUtils.hasText(targetId) ? targetId : "image-" + (targets.size() + 1),
                    intValue(target, "page", 0),
                    intValue(target, "reading_order", intValue(target.path("metadata"), "reading_order", 0)),
                    cropUrl,
                    areaValue(target)
            ));
        }
        return targets;
    }

    private String extractViaOcr(byte[] bytes, String filename, String contentType) {
        if (ocrAsyncEnabled) {
            return extractViaOcrJob(bytes, filename, contentType);
        }
        return extractViaOcrSync(bytes, filename, contentType);
    }

    private String extractViaOcrSync(byte[] bytes, String filename, String contentType) {
        requireLayoutServiceApiKey();
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
            if (isOcrResourceStatus(e.getStatusCode().value())) {
                throw new OcrServiceResourceException(
                        "OCR service is busy or resource constrained: HTTP " + e.getStatusCode(),
                        e
                );
            }
            throw new RuntimeException("OCR service returned HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            log.error("OCR service request failed. url={}, fileName={}", ocrUrl, filename, e);
            throw new OcrServiceResourceException("OCR service is unavailable.", e);
        }
    }

    private String extractViaOcrJob(byte[] bytes, String filename, String contentType) {
        requireLayoutServiceApiKey();
        String jobUrl = resolveOcrJobUrl();
        log.info("Calling OCR job service. url={}, fileName={}, contentType={}, size={}",
                jobUrl, filename, contentType, bytes.length);

        MultiValueMap<String, Object> body = createMultipartBody(bytes, filename, contentType);

        try {
            OcrJobAccepted accepted = ocrRestClient.post()
                    .uri(jobUrl)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(OcrJobAccepted.class);

            if (accepted == null || !StringUtils.hasText(accepted.getJobId())) {
                throw new RuntimeException("OCR job response is empty.");
            }

            OcrResponse response = waitForOcrJob(accepted, filename);
            if (response == null || !StringUtils.hasText(response.getText())) {
                log.warn("OCR job returned empty text. jobId={}, fileName={}, pageCount={}",
                        accepted.getJobId(), filename, response == null ? null : response.getPageCount());
                throw new RuntimeException("OCR job result is empty.");
            }

            log.info("OCR job result received. jobId={}, fileName={}, textLength={}, pageCount={}",
                    accepted.getJobId(), filename, response.getText().length(), response.getPageCount());
            return response.getText();
        } catch (RestClientResponseException e) {
            log.error("OCR job service returned error status. url={}, status={}, body={}",
                    jobUrl, e.getStatusCode(), e.getResponseBodyAsString(), e);
            if (isOcrResourceStatus(e.getStatusCode().value())) {
                throw new OcrServiceResourceException(
                        "OCR job service is busy or resource constrained: HTTP " + e.getStatusCode(),
                        e
                );
            }
            throw new RuntimeException("OCR job service returned HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            log.error("OCR job service request failed. url={}, fileName={}", jobUrl, filename, e);
            throw new OcrServiceResourceException("OCR job service is unavailable.", e);
        }
    }

    private OcrResponse waitForOcrJob(OcrJobAccepted accepted, String filename) {
        String statusUrl = resolveOcrJobStatusUrl(accepted);
        String resultUrl = resolveOcrJobResultUrl(accepted);
        long timeoutMs = Math.max(1L, ocrJobPollTimeoutMs);
        long pollIntervalMs = Math.max(100L, ocrJobPollIntervalMs);
        long deadline = System.nanoTime() + timeoutMs * 1_000_000L;

        while (System.nanoTime() <= deadline) {
            OcrJobStatus status = ocrRestClient.get()
                    .uri(statusUrl)
                    .retrieve()
                    .body(OcrJobStatus.class);

            String state = status == null ? "" : status.getStatus();
            if ("completed".equalsIgnoreCase(state)) {
                return ocrRestClient.get()
                        .uri(resultUrl)
                        .retrieve()
                        .body(OcrResponse.class);
            }
            if ("failed".equalsIgnoreCase(state)) {
                String error = status.getError() == null ? "" : status.getError();
                if (isOcrResourceErrorMessage(error)) {
                    throw new OcrServiceResourceException(
                            "OCR job failed because the OCR service is resource constrained. jobId="
                                    + accepted.getJobId() + ", error=" + error,
                            null
                    );
                }
                throw new RuntimeException("OCR job failed. jobId=" + accepted.getJobId() + ", error=" + error);
            }

            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new OcrServiceResourceException("OCR job polling was interrupted. jobId=" + accepted.getJobId(), e);
            }
        }

        throw new OcrServiceResourceException(
                "OCR job timed out. jobId=" + accepted.getJobId() + ", fileName=" + filename,
                null
        );
    }

    private String resolveOcrJobUrl() {
        if (StringUtils.hasText(ocrJobUrl)) {
            return normalizeOcrJobEndpoint(ocrJobUrl);
        }
        return normalizeOcrJobEndpoint(ocrUrl);
    }

    private String normalizeOcrJobEndpoint(String value) {
        String endpoint = trimTrailingSlash(value);
        if (!StringUtils.hasText(endpoint)) {
            return "";
        }
        if (endpoint.endsWith("/ocr/jobs")) {
            return endpoint;
        }
        if (endpoint.endsWith("/ocr")) {
            return endpoint + "/jobs";
        }
        if (endpoint.endsWith("/jobs")) {
            return endpoint;
        }
        return endpoint + "/ocr/jobs";
    }

    private String resolveOcrJobStatusUrl(OcrJobAccepted accepted) {
        if (StringUtils.hasText(accepted.getStatusUrl())) {
            return accepted.getStatusUrl();
        }
        return resolveOcrJobUrl() + "/" + accepted.getJobId();
    }

    private String resolveOcrJobResultUrl(OcrJobAccepted accepted) {
        if (StringUtils.hasText(accepted.getResultUrl())) {
            return accepted.getResultUrl();
        }
        return resolveOcrJobUrl() + "/" + accepted.getJobId() + "/result";
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replaceAll("/+$", "");
    }

    private boolean isOcrResourceStatus(int statusCode) {
        return statusCode == 408
                || statusCode == 413
                || statusCode == 429
                || statusCode == 503
                || statusCode == 507;
    }

    private boolean isOcrResourceErrorMessage(String error) {
        String lower = error == null ? "" : error.toLowerCase(Locale.ROOT);
        return lower.contains("resource")
                || lower.contains("memory")
                || lower.contains("bad_alloc")
                || lower.contains("mkldnn")
                || lower.contains("onednn")
                || lower.contains("dnnl")
                || lower.contains("busy")
                || lower.contains("timed out");
    }

    private RestClient.Builder withLayoutServiceApiKey(RestClient.Builder builder) {
        if (StringUtils.hasText(layoutServiceApiKey)) {
            return builder.defaultHeader(LAYOUT_API_KEY_HEADER, layoutServiceApiKey);
        }
        return builder;
    }

    private void requireLayoutServiceApiKey() {
        if (!StringUtils.hasText(layoutServiceApiKey)) {
            throw new IllegalStateException("LAYOUT_SERVICE_API_KEY is required for layout/OCR service calls.");
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

    private double areaValue(JsonNode node) {
        double explicitArea = doubleValue(node.path("metadata"), "area", 0.0d);
        if (explicitArea > 0.0d) {
            return explicitArea;
        }

        JsonNode bbox = node.path("bbox");
        if (!bbox.isArray() || bbox.size() != 4) {
            bbox = node.path("metadata").path("bbox");
        }
        if (!bbox.isArray() || bbox.size() != 4) {
            return 0.0d;
        }

        double x1 = bbox.get(0).asDouble(0.0d);
        double y1 = bbox.get(1).asDouble(0.0d);
        double x2 = bbox.get(2).asDouble(0.0d);
        double y2 = bbox.get(3).asDouble(0.0d);
        return Math.abs((x2 - x1) * (y2 - y1));
    }

    private double doubleValue(JsonNode node, String name, double defaultValue) {
        JsonNode value = node == null ? null : node.path(name);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return defaultValue;
        }
        if (value.isNumber()) {
            return value.asDouble(defaultValue);
        }
        if (value.isTextual()) {
            try {
                return Double.parseDouble(value.asText().trim());
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
            Long totalMillis,
            List<ExtractedImageAsset> extractedImages
    ) {
        public ExtractResult {
            extractedImages = extractedImages == null ? List.of() : List.copyOf(extractedImages);
        }
    }

    public record ExtractedImageAsset(
            String targetId,
            int page,
            int readingOrder,
            String contentType,
            double area,
            byte[] bytes
    ) {
    }

    private record LayoutOcrResult(
            String text,
            Long layoutMillis,
            Long ocrMillis,
            boolean usedFallback,
            List<ExtractedImageAsset> images,
            String layoutJobId
    ) {
        private LayoutOcrResult {
            images = images == null ? List.of() : List.copyOf(images);
        }
    }

    private record LayoutAssetsResult(
            List<ExtractedImageAsset> images,
            Long layoutMillis,
            String layoutJobId
    ) {
        private LayoutAssetsResult {
            images = images == null ? List.of() : List.copyOf(images);
        }
    }

    private record OcrTarget(
            String targetId,
            int page,
            int readingOrder,
            String cropUrl
    ) {
    }

    private record ImageTarget(
            String targetId,
            int page,
            int readingOrder,
            String cropUrl,
            double area
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OcrJobAccepted {
        @JsonProperty("job_id")
        private String jobId;

        @JsonProperty("status_url")
        private String statusUrl;

        @JsonProperty("result_url")
        private String resultUrl;

        public String getJobId() {
            return jobId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }

        public String getStatusUrl() {
            return statusUrl;
        }

        public void setStatusUrl(String statusUrl) {
            this.statusUrl = statusUrl;
        }

        public String getResultUrl() {
            return resultUrl;
        }

        public void setResultUrl(String resultUrl) {
            this.resultUrl = resultUrl;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OcrJobStatus {
        private String status;
        private String error;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
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

    private static class OcrServiceResourceException extends RuntimeException {
        private OcrServiceResourceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
