package org.example.backend.campaign.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignRole;
import org.example.backend.campaign.repository.CampaignParticipantRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.campaign.service.CampaignExportService;
import org.example.backend.campaign.service.CampaignPdfReportService;
import org.example.backend.common.security.RoleGuard;
import org.example.backend.user.model.AuthUserDetails;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
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
    private final CampaignParticipantRepository campaignParticipantRepository;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;

    private static final Set<String> DEFAULT_SECTIONS =
            Set.of("campaign", "members", "tasks", "kpi", "esg");

    @GetMapping("/{campaignId}/export.csv")
    public ResponseEntity<byte[]> exportCsv(
            @PathVariable String campaignId,
            @RequestParam(value = "sections", required = false) String sectionsParam,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        Long campaignIdx = toIdx(campaignId);
        requireExportPermission(campaignIdx, user);

        Set<String> sections = parseSections(sectionsParam);
        byte[] body = exportService.exportCsv(campaignIdx, sections);
        String fileName = exportService.resolveFileName(campaignIdx);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
        );

        return ResponseEntity.ok().headers(headers).body(body);
    }

    @GetMapping("/{campaignId}/export.pdf")
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable String campaignId,
            @RequestParam(value = "type", defaultValue = "summary") String type,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        Long campaignIdx = toIdx(campaignId);
        requireExportPermission(campaignIdx, user);

        byte[] body = pdfReportService.generate(campaignIdx, type);
        String fileName = pdfReportService.resolveFileName(campaignIdx, type);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
        );

        return ResponseEntity.ok().headers(headers).body(body);
    }

    private void requireExportPermission(Long campaignIdx, AuthUserDetails user) {
        AuthUserDetails authenticated = RoleGuard.requireManager(user);
        User caller = userRepository.findById(authenticated.getIdx())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "사용자 정보를 찾을 수 없습니다."));
        if (caller.getOrganization() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "소속 조직 정보가 없습니다.");
        }
        boolean isPmOrg = campaignParticipantRepository
                .existsByCampaignIdxAndOrganizationIdxAndCampaignRole(
                        campaignIdx, caller.getOrganization().getIdx(), CampaignRole.PM);
        if (!isPmOrg) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "캠페인의 PM 조직 소속만 내보낼 수 있습니다.");
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

    private Long toIdx(String publicId) {
        return campaignRepository.findByPublicId(publicId)
                .map(Campaign::getIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    }
}
