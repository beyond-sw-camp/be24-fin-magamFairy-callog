package org.example.backend.campaign.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignKpiDto;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.campaign.service.CampaignKpiService;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class CampaignKpiController {

    private final CampaignKpiService kpiService;
    private final CampaignRepository campaignRepository;

    @GetMapping("/campaigns/{campaignId}/kpis")
    public ResponseEntity<?> list(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user) {
        return ResponseEntity.ok(BaseResponse.success(
                kpiService.listKpis(toIdx(campaignId), user.getId())));
    }

    @PostMapping("/campaigns/{campaignId}/kpis")
    public ResponseEntity<?> create(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody CampaignKpiDto.CreateReq req) {
        return ResponseEntity.ok(BaseResponse.success(
                kpiService.createKpi(toIdx(campaignId), user.getId(), req)));
    }

    @PatchMapping("/campaigns/{campaignId}/kpis/{kpiId}")
    public ResponseEntity<?> updateMeta(
            @PathVariable String campaignId,
            @PathVariable Long kpiId,
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody CampaignKpiDto.UpdateMetaReq req) {
        return ResponseEntity.ok(BaseResponse.success(
                kpiService.updateMeta(toIdx(campaignId), kpiId, user.getId(), req)));
    }

    @PatchMapping("/campaigns/{campaignId}/kpis/{kpiId}/actual")
    public ResponseEntity<?> updateActual(
            @PathVariable String campaignId,
            @PathVariable Long kpiId,
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody CampaignKpiDto.UpdateActualReq req) {
        return ResponseEntity.ok(BaseResponse.success(
                kpiService.updateActual(toIdx(campaignId), kpiId, user.getId(), req)));
    }

    @DeleteMapping("/campaigns/{campaignId}/kpis/{kpiId}")
    public ResponseEntity<?> delete(
            @PathVariable String campaignId,
            @PathVariable Long kpiId,
            @AuthenticationPrincipal AuthUserDetails user) {
        kpiService.deleteKpi(toIdx(campaignId), kpiId, user.getId());
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @PatchMapping("/campaigns/{campaignId}/kpis/analysis")
    public ResponseEntity<?> updateAnalysis(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody CampaignKpiDto.UpdateAnalysisReq req) {
        kpiService.updateAnalysis(toIdx(campaignId), user.getId(), req);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @GetMapping("/campaigns/kpis/frameworks")
    public ResponseEntity<?> listFrameworks() {
        return ResponseEntity.ok(BaseResponse.success(kpiService.listFrameworks()));
    }

    @PostMapping("/campaigns/{campaignId}/kpis/import-framework")
    public ResponseEntity<?> importFramework(
            @PathVariable String campaignId,
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody CampaignKpiDto.ImportFrameworkReq req) {
        return ResponseEntity.ok(BaseResponse.success(
                kpiService.importFramework(toIdx(campaignId), user.getId(), req)));
    }

    private Long toIdx(String publicId) {
        return campaignRepository.findByPublicId(publicId)
                .map(Campaign::getIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    }
}
