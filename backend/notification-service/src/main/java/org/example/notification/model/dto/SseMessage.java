package org.example.notification.model.dto;

// SSE로 브라우저에 전달할 실시간 알림 메시지 DTO입니다.
public record SseMessage(
        Long userId,
        String eventName,
        Object data,
        boolean broadcast
) {
    public static SseMessage toUser(Long userId, String eventName, Object data) {
        return new SseMessage(userId, eventName, data, false);
    }

    public static SseMessage broadcast(String eventName, Object data) {
        return new SseMessage(null, eventName, data, true);
    }
}
