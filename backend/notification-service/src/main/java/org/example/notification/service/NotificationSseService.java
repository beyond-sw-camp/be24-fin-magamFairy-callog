package org.example.notification.service;

import org.example.notification.model.dto.SseMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// 브라우저와 SSE 연결을 유지하고 실시간 알림을 전송하는 서비스입니다.
@Service
public class NotificationSseService {
    private static final long SSE_TIMEOUT_MILLIS = 60L * 60L * 1000L;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // 특정 사용자의 SSE 연결을 등록하고 연결 종료 시 자동으로 제거합니다.
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(error -> remove(userId, emitter));

        return emitter;
    }

    // 특정 사용자에게 알림 이벤트를 전송합니다.
    public void publishToUser(Long userId, Object data) {
        sendToUser(userId, SseMessage.toUser(userId, "notification", data));
    }

    // 현재 서버에 연결된 모든 사용자에게 브로드캐스트 이벤트를 전송합니다.
    public void broadcast(String eventName, Object data) {
        SseMessage message = SseMessage.broadcast(eventName, data);
        emitters.keySet().forEach(userId -> sendToUser(userId, message));
    }

    // 현재 서버에 연결된 특정 사용자의 모든 SSE 연결로 메시지를 보냅니다.
    private void sendToUser(Long userId, SseMessage message) {
        emitters.getOrDefault(userId, List.of())
                .forEach(emitter -> sendToEmitter(userId, emitter, message));
    }

    // 단일 SSE 연결로 메시지를 보내고 실패한 연결은 제거합니다.
    private void sendToEmitter(Long userId, SseEmitter emitter, SseMessage message) {
        try {
            emitter.send(SseEmitter.event()
                    .name(message.eventName())
                    .data(message.data()));
        } catch (IOException | IllegalStateException exception) {
            remove(userId, emitter);
        }
    }

    // 끊어진 SSE 연결을 사용자 연결 목록에서 제거합니다.
    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return;
        }
        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) {
            emitters.remove(userId);
        }
    }
}
