package org.example.backend.campaign.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.CampaignKpiDto;
import org.example.backend.campaign.service.CampaignKpiService;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CampaignKpiController {

    private final CampaignKpiService kpiService;

    @GetMapping("/campaigns/{campaignId}/kpis")
    public ResponseEntity<?> list(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal AuthUserDetails user) {
        return ResponseEntity.ok(BaseResponse.success(
                kpiService.listKpis(campaignId, user.getId())));
    }

    @PostMapping("/campaigns/{campaignId}/kpis")
    public ResponseEntity<?> create(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody CampaignKpiDto.CreateReq req) {
        return ResponseEntity.ok(BaseResponse.success(
                kpiService.createKpi(campaignId, user.getId(), req)));
    }

    @PatchMapping("/campaigns/{campaignId}/kpis/{kpiId}")
    public ResponseEntity<?> updateMeta(
            @PathVariable Long campaignId,
            @PathVariable Long kpiId,
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody CampaignKpiDto.UpdateMetaReq req) {
        return ResponseEntity.ok(BaseResponse.success(
                kpiService.updateMeta(campaignId, kpiId, user.getId(), req)));
    }

    @PatchMapping("/campaigns/{campaignId}/kpis/{kpiId}/actual")
    public ResponseEntity<?> updateActual(
            @PathVariable Long campaignId,
            @PathVariable Long kpiId,
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody CampaignKpiDto.UpdateActualReq req) {
        return ResponseEntity.ok(BaseResponse.success(
                kpiService.updateActual(campaignId, kpiId, user.getId(), req)));
    }

    @DeleteMapping("/campaigns/{campaignId}/kpis/{kpiId}")
    public ResponseEntity<?> delete(
            @PathVariable Long campaignId,
            @PathVariable Long kpiId,
            @AuthenticationPrincipal AuthUserDetails user) {
        kpiService.deleteKpi(campaignId, kpiId, user.getId());
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @PatchMapping("/campaigns/{campaignId}/kpis/analysis")
    public ResponseEntity<?> updateAnalysis(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody CampaignKpiDto.UpdateAnalysisReq req) {
        kpiService.updateAnalysis(campaignId, user.getId(), req);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @GetMapping("/campaigns/kpis/frameworks")
    public ResponseEntity<?> listFrameworks() {
        return ResponseEntity.ok(BaseResponse.success(kpiService.listFrameworks()));
    }

    @PostMapping("/campaigns/{campaignId}/kpis/import-framework")
    public ResponseEntity<?> importFramework(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody CampaignKpiDto.ImportFrameworkReq req) {
        return ResponseEntity.ok(BaseResponse.success(
                kpiService.importFramework(campaignId, user.getId(), req)));
    }
}
