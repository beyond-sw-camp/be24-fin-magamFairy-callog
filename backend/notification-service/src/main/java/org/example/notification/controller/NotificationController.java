package org.example.notification.controller;


import lombok.RequiredArgsConstructor;
import org.example.notification.common.security.AuthUser;
import org.example.notification.model.dto.NotificationDto;
import org.example.notification.service.NotificationCommandService;
import org.example.notification.service.NotificationQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// 알림 목록 조회와 읽음 처리를 담당하는 REST 컨트롤러
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

    private Long requireUserId(AuthUser user) {
        if(user == null || user.userId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "알림 : 유저 ID가 필요합니다.");
        }
        return user.userId();
    }
}
