package org.example.notification.controller;

import lombok.RequiredArgsConstructor;
import org.example.notification.common.security.AuthUser;
import org.example.notification.service.NotificationSseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// 브라우저와 알림 SSE 연결을 생성하는 컨트롤러입니다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationSseController {
    private final NotificationSseService sseService;

    // 현재 사용자의 SSE 연결을 생성하고 서버 푸시 알림을 받을 수 있게 합니다.
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal AuthUser user) {
        return sseService.subscribe(requireUserId(user));
    }

    // 인증된 사용자 ID가 없으면 401 예외를 발생시킵니다.
    private Long requireUserId(AuthUser user) {
        if (user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user is required.");
        }
        return user.userId();
    }
}