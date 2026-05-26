package org.example.backend.notification.service;

/**
 * Pod 간 SSE 이벤트 전달용 메시지. Redis 채널로 발행/구독된다.
 * userIdx == null 이면 전체 broadcast (calendar.refresh 등).
 */
public record SseMessage(Long userIdx, String eventName, Object data, boolean broadcast) {

    public static SseMessage toUser(Long userIdx, String eventName, Object data) {
        return new SseMessage(userIdx, eventName, data, false);
    }

    public static SseMessage broadcast(String eventName, Object data) {
        return new SseMessage(null, eventName, data, true);
    }
}