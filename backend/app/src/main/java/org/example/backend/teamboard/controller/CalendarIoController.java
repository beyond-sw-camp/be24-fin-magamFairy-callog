package org.example.backend.teamboard.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.common.security.RoleGuard;
import org.example.backend.teamboard.service.CalendarIoService;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** 구글 캘린더 형식(.ics) 가져오기/내보내기. */
@RestController
@RequiredArgsConstructor
public class CalendarIoController {

    private final CalendarIoService calendarIoService;

    /** 내보내기 — 지정 날짜 범위·유형의 내 일정을 .ics 파일로 다운로드. */
    @GetMapping("/calendar/export")
    public ResponseEntity<byte[]> export(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "task,milestone,deadline") String types
    ) {
        RoleGuard.requireAuthenticated(user);
        Set<String> typeSet = Arrays.stream(types.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        byte[] ics = calendarIoService.export(user.getIdx(), LocalDate.parse(from), LocalDate.parse(to), typeSet);
        String filename = "callog-" + from.replace("-", "") + "-" + to.replace("-", "") + ".ics";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/calendar;charset=UTF-8"))
                .body(ics);
    }

    /** 가져오기 — .ics 업로드 → 개인 업무로 추가. mode=overwrite|append. */
    @PostMapping("/calendar/import")
    public ResponseEntity<BaseResponse> importIcs(
            @AuthenticationPrincipal AuthUserDetails user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "append") String mode
    ) {
        RoleGuard.requireAuthenticated(user);
        return ResponseEntity.ok(BaseResponse.success(
                calendarIoService.importIcs(user.getIdx(), file, mode)
        ));
    }
}
