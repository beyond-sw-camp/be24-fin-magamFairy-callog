package org.example.backend.kpi.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.kpi.dto.CreateKpiTemplateRequest;
import org.example.backend.kpi.model.TemplateScope;
import org.example.backend.kpi.service.KpiTemplateService;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/kpi-templates")
public class KpiTemplateController {

    private final KpiTemplateService templateService;

    @GetMapping
    public ResponseEntity<BaseResponse> list(
            @RequestParam(required = false) TemplateScope scope,
            @RequestParam(required = false) Long orgId,
            @AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(templateService.list(scope, orgId)));
    }

    @PostMapping
    public ResponseEntity<BaseResponse> create(
            @RequestBody CreateKpiTemplateRequest req,
            @AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(templateService.create(user.getIdx(), req)));
    }

    @PostMapping("/{id}/instantiate")
    public ResponseEntity<BaseResponse> instantiate(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserDetails user) {
        requireAuth(user);
        return ResponseEntity.ok(BaseResponse.success(templateService.instantiate(user.getIdx(), id)));
    }

    private void requireAuth(AuthUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 유저 정보가 없습니다.");
        }
    }
}
