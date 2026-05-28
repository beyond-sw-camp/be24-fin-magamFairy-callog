package com.example.adcheck.controller;

import com.example.adcheck.analysis.service.AdCheckAnalysisMongoStorageService;
import com.example.adcheck.model.AdCheckDto;
import com.example.adcheck.service.AiJudgeFileCheckService;
import com.example.adcheck.service.AiJudgeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/multi/aijudge", "/aijudge"})
public class AiJudgeController {

    private final AiJudgeService aiJudgeService;
    private final AiJudgeFileCheckService aiJudgeFileCheckService;
    private final AdCheckAnalysisMongoStorageService adCheckAnalysisMongoStorageService;
    private final ObjectMapper objectMapper;

    public AiJudgeController(
            AiJudgeService aiJudgeService,
            AiJudgeFileCheckService aiJudgeFileCheckService,
            AdCheckAnalysisMongoStorageService adCheckAnalysisMongoStorageService,
            ObjectMapper objectMapper
    ) {
        this.aiJudgeService = aiJudgeService;
        this.aiJudgeFileCheckService = aiJudgeFileCheckService;
        this.adCheckAnalysisMongoStorageService = adCheckAnalysisMongoStorageService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/check")
    public AdCheckDto.Res check(@RequestBody AdCheckDto.Req req) {
        if (req == null || req.copy() == null || req.copy().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "copy is required.");
        }

        return aiJudgeService.check(req.copy());
    }

    @PostMapping(value = "/check/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdCheckDto.FileCheckRes checkFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "context", required = false) String context,
            @RequestParam(value = "analysisJobId", required = false) String analysisJobId,
            @RequestParam(value = "campaignId", required = false) String campaignId,
            @RequestHeader(value = "X-User-Idx", required = false) String requesterUserIdx,
            @RequestHeader(value = "X-User-Id", required = false) String requesterLoginId,
            @RequestHeader(value = "X-User-Email", required = false) String requesterEmail,
            @RequestHeader(value = "X-User-Role", required = false) String requesterRole,
            @RequestHeader(value = "X-User-Name", required = false) String requesterName,
            @RequestHeader(value = "X-User-OrgType", required = false) String requesterOrgType
    ) {
        Map<String, Object> mergedContext = buildRequestContext(
                context,
                campaignId,
                requesterUserIdx,
                requesterLoginId,
                requesterEmail,
                requesterRole,
                requesterName,
                requesterOrgType
        );
        return aiJudgeFileCheckService.checkFile(file, serializeContext(mergedContext), analysisJobId);
    }

    @GetMapping("/analyses/{analysisJobId}")
    public AdCheckDto.FileCheckRes analysisDetail(@PathVariable String analysisJobId) {
        Optional<AdCheckDto.FileCheckRes> analysis = adCheckAnalysisMongoStorageService.findByAnalysisJobId(analysisJobId);
        return analysis.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "analysis result not found."));
    }

    @ExceptionHandler(AiJudgeFileCheckService.FileCheckException.class)
    public ResponseEntity<AdCheckDto.FileCheckRes> handleFileCheckException(
            AiJudgeFileCheckService.FileCheckException e
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getResponse());
    }

    private Map<String, Object> buildRequestContext(
            String rawContext,
            String campaignId,
            String requesterUserIdx,
            String requesterLoginId,
            String requesterEmail,
            String requesterRole,
            String requesterName,
            String requesterOrgType
    ) {
        Map<String, Object> context = parseContext(rawContext);

        putIfHasText(context, "campaignId", campaignId);
        putIfHasText(context, "requesterUserIdx", requesterUserIdx);
        putIfHasText(context, "requesterLoginId", requesterLoginId);
        putIfHasText(context, "requesterEmail", requesterEmail);
        putIfHasText(context, "requesterRole", requesterRole);
        putIfHasText(context, "requesterName", requesterName);
        putIfHasText(context, "requesterOrgType", requesterOrgType);

        return context;
    }

    private String serializeContext(Map<String, Object> context) {
        if (context.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(context);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "context cannot be serialized.", e);
        }
    }

    private Map<String, Object> parseContext(String rawContext) {
        if (!hasText(rawContext)) {
            return new LinkedHashMap<>();
        }

        try {
            Map<String, Object> parsed = objectMapper.readValue(rawContext, new TypeReference<Map<String, Object>>() {});
            return parsed == null ? new LinkedHashMap<>() : new LinkedHashMap<>(parsed);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "context must be a JSON object.", e);
        }
    }

    private void putIfHasText(Map<String, Object> context, String key, String value) {
        if (hasText(value)) {
            context.put(key, value.trim());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
