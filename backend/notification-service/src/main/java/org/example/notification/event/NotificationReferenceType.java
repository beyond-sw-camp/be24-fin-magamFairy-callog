package org.example.notification.event;

public enum NotificationReferenceType {
    // 업무 알림의 원본 대상
    TASK,

    // 캠페인 알림의 원본 대상
    CAMPAIGN,

    // 캠페인 초대 알림의 원본 대상
    CAMPAIGN_INVITATION,

    // 광고/콘텐츠 검수 알림의 원본 대상
    AD_REVIEW,

    // 마감 임박 또는 마감 초과 알림의 원본 대상
    DEADLINE,

    // 시스템 공지 또는 외부 참조가 없는 알림
    SYSTEM
}
