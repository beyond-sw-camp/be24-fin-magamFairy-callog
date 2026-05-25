package org.example.notification.controller;

import lombok.RequiredArgsConstructor;
import org.example.notification.common.security.AuthUser;
import org.example.notification.model.dto.NotificationDto;
import org.example.notification.service.NotificationOrganizationPolicyService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// 프론트의 조직 알림 정책 API를 처리하는 컨트롤러입니다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications/admin-policies")
public class NotificationOrganizationPolicyController {
    private final NotificationOrganizationPolicyService policyService;

    // 현재 사용자의 조직 알림 정책 목록을 조회합니다.
    @GetMapping
    public NotificationDto.AdminPolicyRes list(@AuthenticationPrincipal AuthUser user) {
        return policyService.list(requireOrganizationId(user), requireRole(user));
    }

    // 현재 사용자의 조직 알림 정책을 추가하거나 수정합니다.
    @PatchMapping
    public NotificationDto.AdminPolicyRes update(
            @AuthenticationPrincipal AuthUser user,
            @RequestBody NotificationDto.AdminPolicyReq request
    ) {
        return policyService.update(requireOrganizationId(user), requireRole(user), request);
    }

    private Long requireOrganizationId(AuthUser user) {
        if (user == null || user.organizationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "organizationId is required.");
        }
        return user.organizationId();
    }

    private String requireRole(AuthUser user) {
        if (user == null || user.role() == null || user.role().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user role is required.");
        }
        return user.role();
    }
}
