package org.example.backend.campaign.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.CampaignMember;
import org.example.backend.campaign.model.CampaignRole;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.campaign.service.CampaignExportService;
import org.example.backend.campaign.service.CampaignPdfReportService;
import org.example.backend.common.security.RoleGuard;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/campaigns")
public class CampaignExportController {

    private final CampaignExportService exportService;
    private final CampaignPdfReportService pdfReportService;
    private final CampaignMemberRepository campaignMemberRepository;

    private static final Set<String> DEFAULT_SECTIONS =
            Set.of("campaign", "members", "tasks", "kpi", "esg");

    @GetMapping("/{campaignId}/export.csv")
    public ResponseEntity<byte[]> exportCsv(
            @PathVariable Long campaignId,
            @RequestParam(value = "sections", required = false) String sectionsParam,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        requireExportPermission(campaignId, user);

        Set<String> sections = parseSections(sectionsParam);
        byte[] body = exportService.exportCsv(campaignId, sections);
        String fileName = exportService.resolveFileName(campaignId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
        );

        return ResponseEntity.ok().headers(headers).body(body);
    }

    /** PDF 보고서 다운로드. type=summary(1쪽) | full(다중 페이지) */
    @GetMapping("/{campaignId}/export.pdf")
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable Long campaignId,
            @RequestParam(value = "type", defaultValue = "summary") String type,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        requireExportPermission(campaignId, user);

        byte[] body = pdfReportService.generate(campaignId, type);
        String fileName = pdfReportService.resolveFileName(campaignId, type);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
        );

        return ResponseEntity.ok().headers(headers).body(body);
    }

    /** 내보내기 권한: 글로벌 ROLE_MANAGER 또는 ROLE_GENERAL_MANAGER 이면서, 해당 캠페인의 PM 인 경우만 허용. */
    private void requireExportPermission(Long campaignId, AuthUserDetails user) {
        AuthUserDetails authenticated = RoleGuard.requireManager(user);
        CampaignMember member = campaignMemberRepository
                .findByCampaignIdxAndUserIdx(campaignId, authenticated.getIdx())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "캠페인 멤버가 아닙니다."));
        if (member.getCampaignRole() != CampaignRole.PM) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "해당 캠페인의 PM만 내보낼 수 있습니다.");
        }
    }

    private Set<String> parseSections(String param) {
        if (param == null || param.isBlank()) return DEFAULT_SECTIONS;
        Set<String> result = new java.util.HashSet<>();
        for (String s : param.split(",")) {
            String trimmed = s.trim().toLowerCase();
            if (DEFAULT_SECTIONS.contains(trimmed)) result.add(trimmed);
        }
        return result.isEmpty() ? DEFAULT_SECTIONS : result;
    }
}
