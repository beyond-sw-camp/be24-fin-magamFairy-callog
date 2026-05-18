package org.example.backend.adcheck.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
public class TextExtractorService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".bmp", ".jpeg", ".jpg", ".png", ".tif", ".tiff", ".webp"
    );

    private final RestClient ocrRestClient;

    @Value("${custom.ocr.url}")
    private String ocrUrl;

    public TextExtractorService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(120000);
        factory.setProxy(Proxy.NO_PROXY);
        this.ocrRestClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    public String extract(MultipartFile file) throws IOException {
        String contentType = file.getContentType() != null ? file.getContentType() : "";
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        byte[] bytes = file.getBytes();

        if (isPlainText(contentType, filename)) {
            return new String(bytes, StandardCharsets.UTF_8);
        }

        if (isPdf(contentType, filename)) {
            String text = extractFromPdf(bytes);
            if (StringUtils.hasText(text)) {
                return text;
            }
            return extractViaOcr(bytes, filename, contentType);
        }

        if (isImage(contentType, filename)) {
            return extractViaOcr(bytes, filename, contentType);
        }

        throw new IllegalArgumentException("Unsupported file type: " + contentType);
    }

    private String extractFromPdf(byte[] bytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String extractViaOcr(byte[] bytes, String filename, String contentType) {
        log.info("Calling OCR service. url={}, fileName={}, contentType={}, size={}", ocrUrl, filename, contentType, bytes.length);

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
