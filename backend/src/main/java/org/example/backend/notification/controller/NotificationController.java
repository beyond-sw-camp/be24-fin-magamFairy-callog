package org.example.backend.notification.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.security.RoleGuard;
import org.example.backend.notification.model.NotificationDto;
import org.example.backend.notification.service.NotificationService;
import org.example.backend.notification.service.NotificationSseService;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final NotificationSseService notificationSseService;

    @GetMapping("/list")
    public ResponseEntity<BaseResponse<NotificationDto.ListRes>> list(
            @RequestParam(required = false) Integer count,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(notificationService.list(user.getIdx(), count)));
    }

    @PatchMapping("/confirm")
    public ResponseEntity<BaseResponse<NotificationDto.Res>> confirm(
            @RequestParam Long idx,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(notificationService.confirm(user.getIdx(), idx)));
    }

    @PatchMapping("/confirm-all")
    public ResponseEntity<BaseResponse<NotificationDto.ListRes>> confirmAll(
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(notificationService.confirmAll(user.getIdx())));
    }

    @GetMapping(path = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal AuthUserDetails user) {
        RoleGuard.requireAuthenticated(user);
        return notificationSseService.subscribe(user.getIdx());
    }
}
