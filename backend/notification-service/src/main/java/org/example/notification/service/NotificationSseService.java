package org.example.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.notification.model.dto.SseMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// 브라우저 SSE 연결을 관리하고 Redis Pub/Sub으로 모든 Pod에 실시간 알림을 전파하는 서비스입니다.
@Service
@RequiredArgsConstructor
public class NotificationSseService {
    public static final String NOTIFICATION_SSE_CHANNEL = "notification:sse";
    private static final long SSE_TIMEOUT_MILLIS = 60L * 60L * 1000L;
    private static final long HEARTBEAT_INTERVAL_MILLIS = 25_000L;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
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

    // 특정 사용자에게 알림 생성 이벤트를 전송합니다.
    public void publishToUser(Long userId, Object data) {
        publish(SseMessage.toUser(userId, "notification.created", data));
    }

    // 모든 Pod에 브로드캐스트 이벤트를 전파합니다.
    public void broadcast(String eventName, Object data) {
        publish(SseMessage.broadcast(eventName, data));
    }

    // Redis에서 수신한 SSE 메시지를 현재 Pod에 연결된 사용자에게만 전달합니다.
    public void deliverLocal(SseMessage message) {
        if (message.broadcast()) {
            emitters.keySet().forEach(userId -> sendToLocalUser(userId, message));
            return;
        }
        sendToLocalUser(message.userId(), message);
    }

    // 연결이 유휴 상태로 끊기지 않도록 모든 로컬 SSE 연결에 주기적으로 heartbeat를 보냅니다.
    @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MILLIS)
    public void sendHeartbeat() {
        emitters.forEach((userId, userEmitters) ->
                userEmitters.forEach(emitter ->
                        sendToEmitter(userId, emitter, SseMessage.toUser(userId, "heartbeat", "ping"))));
    }

    // SSE 메시지를 Redis 채널로 발행해서 모든 notification-service Pod가 받게 합니다.
    private void publish(SseMessage message) {
        try {
            stringRedisTemplate.convertAndSend(
                    NOTIFICATION_SSE_CHANNEL,
                    objectMapper.writeValueAsString(message)
            );
        } catch (JsonProcessingException | RuntimeException exception) {
            deliverLocal(message);
        }
    }

    // 현재 Pod에 연결된 특정 사용자의 모든 SSE 연결로 메시지를 보냅니다.
    private void sendToLocalUser(Long userId, SseMessage message) {
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

    // 닫힌 SSE 연결을 사용자 연결 목록에서 제거합니다.
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
