package org.example.notification.event;

public enum NotificationEventType {
    // 업무가 사용자에게 배정됨
    TASK_ASSIGNED,

    // 업무 진행 상태가 변경됨
    TASK_STATUS_CHANGED,

    // 업무 제목, 내용, 담당자, 일정 등이 수정됨
    TASK_UPDATED,

    // 캠페인 참여 초대가 생성됨
    CAMPAIGN_INVITED,

    // 캠페인 초대가 승인됨
    CAMPAIGN_INVITATION_ACCEPTED,

    // 캠페인 초대가 거절됨
    CAMPAIGN_INVITATION_REJECTED,

    // 캠페인에 새 구성원이 추가됨
    CAMPAIGN_MEMBER_ADDED,

    // 광고/콘텐츠 검수 요청이 생성됨
    REVIEW_REQUESTED,

    // 광고/콘텐츠 검수가 승인됨
    REVIEW_APPROVED,

    // 광고/콘텐츠 검수가 반려됨
    REVIEW_REJECTED,

    // 마감 24시간 전 알림
    DEADLINE_24H,

    // 마감 1시간 전 알림
    DEADLINE_1H,

    // 마감 시간이 지났다는 알림
    DEADLINE_OVERDUE,

    // 시스템 공지 또는 특정 도메인에 속하지 않는 알림
    SYSTEM
}
