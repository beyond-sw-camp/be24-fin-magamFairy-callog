package org.example.backend.teamboard.model;

public enum TaskStatus {
    BACKLOG,      // 백로그
    TODO,         // 할 일
    IN_PROGRESS,  // 진행 중
    REVIEW,       // 검수 중
    DONE,         // 완료
    BLOCKED       // 차단
}