package org.example.backend.kpi.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.kpi.dto.CreateOrganizationKpiRequest;
import org.example.backend.kpi.dto.UpdateOrganizationKpiRequest;
import org.example.backend.kpi.model.GoalStatus;
import org.example.backend.kpi.service.OrganizationKpiService;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/organization-kpis")
public class OrganizationKpiController {

    private final OrganizationKpiService kpiService;

    @GetMapping
    public ResponseEntity<BaseResponse> list(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Long owner,
            @RequestParam(required = false) GoalStatus status,
            @AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(kpiService.list(user.getIdx(), period, owner, status)));
    }

    @GetMapping("/parents")
    public ResponseEntity<BaseResponse> listParents(
            @RequestParam(required = false) Long orgId,
            @AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(kpiService.listParentCandidates(user.getIdx(), orgId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(kpiService.get(id)));
    }

    @PostMapping
    public ResponseEntity<BaseResponse> create(
            @RequestBody CreateOrganizationKpiRequest req,
            @AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(kpiService.create(user.getIdx(), req)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BaseResponse> update(
            @PathVariable Long id,
            @RequestBody UpdateOrganizationKpiRequest req,
            @AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(kpiService.update(user.getIdx(), id, req)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BaseResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody StatusReq req,
            @AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(
                kpiService.updateStatus(user.getIdx(), id, req == null ? null : req.status())));
    }

    public record StatusReq(GoalStatus status) {}

    private void requireAuth(AuthUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 유저 정보가 없습니다.");
        }
    }
}
