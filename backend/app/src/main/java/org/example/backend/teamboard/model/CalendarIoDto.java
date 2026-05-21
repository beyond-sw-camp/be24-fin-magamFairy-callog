package org.example.backend.teamboard.model;

/** 캘린더 가져오기/내보내기 (.ics) 응답 DTO. */
public class CalendarIoDto {

    /** 가져오기 결과 — 추가된 개수, (덮어쓰기 시) 삭제된 개수. */
    public record ImportResult(
            int importedCount,
            int deletedCount,
            String mode
    ) {}
}
