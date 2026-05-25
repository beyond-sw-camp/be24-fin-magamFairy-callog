package org.example.notification.event;

public enum NotificationSeverity {
    // 참고용 알림: 즉시 확인하지 않아도 되는 낮은 중요도
    LOW,

    // 일반 알림: 대부분의 업무 알림에 사용하는 기본 중요도
    NORMAL,

    // 중요 알림: 검수 반려, 마감 임박처럼 사용자가 빨리 봐야 하는 알림
    HIGH,

    // 긴급 알림: 장애, 보안, 마감 초과처럼 즉시 확인이 필요한 알림
    CRITICAL
}
