package org.example.notification.controller;

import lombok.RequiredArgsConstructor;
import org.example.notification.common.security.AuthUser;
import org.example.notification.model.dto.NotificationDto;
import org.example.notification.service.NotificationCommandService;
import org.example.notification.service.NotificationQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// 알림 목록 조회와 읽음 처리를 담당하는 REST 컨트롤러입니다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationQueryService queryService;
    private final NotificationCommandService commandService;

    // 현재 사용자의 알림 목록과 읽지 않은 알림 수를 조회합니다.
    @GetMapping("/list")
    public NotificationDto.ListRes list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) Integer count
    ) {
        return queryService.list(requireUserId(user), count);
    }

    // 현재 사용자의 특정 알림 한 건을 읽음 처리합니다.
    @PatchMapping("/confirm")
    public NotificationDto.Res confirm(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam Long idx
    ) {
        return commandService.markAsRead(requireUserId(user), idx);
    }

    // 현재 사용자의 읽지 않은 모든 알림을 읽음 처리합니다.
    @PatchMapping("/confirm-all")
    public NotificationDto.ListRes confirmAll(@AuthenticationPrincipal AuthUser user) {
        Long userId = requireUserId(user);
        commandService.markAllAsRead(userId);
        return queryService.list(userId, null);
    }

    // 인증된 사용자 ID가 없으면 401 예외를 발생시킵니다.
    private Long requireUserId(AuthUser user) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user is required.");
        }
        return user.userId();
    }
}
