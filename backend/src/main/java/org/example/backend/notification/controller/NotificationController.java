package org.example.backend.notification.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.model.BaseResponseStatus;
import org.example.backend.common.security.RoleGuard;
import org.example.backend.notification.model.NotificationDto;
import org.example.backend.notification.service.NotificationService;
import org.example.backend.notification.service.NotificationSettingsService;
import org.example.backend.notification.service.NotificationSseService;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final NotificationSseService notificationSseService;
    private final NotificationSettingsService notificationSettingsService;

    @GetMapping("/list")
    public ResponseEntity<BaseResponse<NotificationDto.ListRes>> list(
            @RequestParam(required = false) Integer count,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(
                BaseResponse.success(BaseResponseStatus.LIST_SUCCESS, notificationService.list(user.getIdx(), count))
        );
    }

    @PatchMapping("/confirm")
    public ResponseEntity<BaseResponse<NotificationDto.Res>> confirm(
            @RequestParam Long idx,
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(
                BaseResponse.success(BaseResponseStatus.SUCCESS, notificationService.confirm(user.getIdx(), idx))
        );
    }

    @PatchMapping("/confirm-all")
    public ResponseEntity<BaseResponse<NotificationDto.ListRes>> confirmAll(
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(
                BaseResponse.success(BaseResponseStatus.SUCCESS, notificationService.confirmAll(user.getIdx()))
        );
    }

    @GetMapping(path = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal AuthUserDetails user) {
        RoleGuard.requireAuthenticated(user);
        return notificationSseService.subscribe(user.getIdx());
    }

    @GetMapping("/settings")
    public ResponseEntity<BaseResponse<NotificationDto.SettingRes>> getSettings(
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(
                BaseResponse.success(BaseResponseStatus.SUCCESS, notificationSettingsService.getSetting(user))
        );
    }

    @PatchMapping("/settings")
    public ResponseEntity<BaseResponse<NotificationDto.SettingRes>> updateSettings(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody NotificationDto.SettingReq request
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(
                BaseResponse.success(BaseResponseStatus.SUCCESS, notificationSettingsService.updateSetting(user, request))
        );
    }

    @GetMapping("/admin-policies")
    public ResponseEntity<BaseResponse<NotificationDto.AdminPolicyRes>> getAdminPolicies(
            @AuthenticationPrincipal AuthUserDetails user
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(
                BaseResponse.success(BaseResponseStatus.LIST_SUCCESS, notificationSettingsService.listAdminPolicies(user))
        );
    }

    @PatchMapping("/admin-policies")
    public ResponseEntity<BaseResponse<NotificationDto.AdminPolicyRes>> updateAdminPolicies(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestBody NotificationDto.AdminPolicyReq request
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(
                BaseResponse.success(BaseResponseStatus.SUCCESS, notificationSettingsService.updateAdminPolicies(user, request))
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<BaseResponse<Void>> handleResponseStatusException(ResponseStatusException exception) {
        BaseResponseStatus status = resolveResponseStatus(exception);

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(BaseResponse.fail(status));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleException(Exception exception) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.fail(BaseResponseStatus.FAIL));
    }

    private BaseResponseStatus resolveResponseStatus(ResponseStatusException exception) {
        if (exception.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
            return BaseResponseStatus.NO_SUCH_ELEMENT;
        }

        if (exception.getStatusCode().isSameCodeAs(HttpStatus.FORBIDDEN)) {
            return BaseResponseStatus.ACCESS_DENIED;
        }

        return BaseResponseStatus.FAIL;
    }
}
