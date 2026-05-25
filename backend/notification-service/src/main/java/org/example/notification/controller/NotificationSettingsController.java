package org.example.notification.controller;

import lombok.RequiredArgsConstructor;
import org.example.notification.common.security.AuthUser;
import org.example.notification.model.dto.NotificationSettingDto;
import org.example.notification.service.NotificationSettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// 사용자 알림 수신 설정 조회와 수정을 담당하는 REST 컨트롤러입니다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications/settings")
public class NotificationSettingsController {
    private final NotificationSettingsService settingsService;

    // 현재 사용자의 알림 수신 설정을 조회합니다.
    @GetMapping
    public NotificationSettingDto.Res get(@AuthenticationPrincipal AuthUser user) {
        return settingsService.get(requireUserId(user));
    }

    // 현재 사용자의 알림 수신 설정을 부분 수정합니다.
    @PatchMapping
    public NotificationSettingDto.Res update(
            @AuthenticationPrincipal AuthUser user,
            @RequestBody NotificationSettingDto.Req request
    ) {
        return settingsService.update(requireUserId(user), request);
    }

    // 인증된 사용자 ID가 없으면 401 예외를 발생시킵니다.
    private Long requireUserId(AuthUser user) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user is required.");
        }
        return user.userId();
    }
}