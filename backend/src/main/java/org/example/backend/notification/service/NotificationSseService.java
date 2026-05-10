package org.example.backend.notification.service;

import org.example.backend.notification.model.NotificationDto;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationSseService {
    private static final long SSE_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userIdx) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emitters.computeIfAbsent(userIdx, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userIdx, emitter));
        emitter.onTimeout(() -> removeEmitter(userIdx, emitter));
        emitter.onError(error -> removeEmitter(userIdx, emitter));

        sendEvent(userIdx, emitter, "heartbeat", "connected");
        return emitter;
    }

    public void sendToUser(Long userIdx, NotificationDto.Res notification) {
        sendToUser(userIdx, "notification.created", notification);
    }

    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        emitters.forEach((userIdx, userEmitters) ->
                userEmitters.forEach(emitter -> sendEvent(userIdx, emitter, "heartbeat", "ping")));
    }

    private void sendToUser(Long userIdx, String eventName, Object data) {
        List<SseEmitter> userEmitters = emitters.get(userIdx);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }

        userEmitters.forEach(emitter -> sendEvent(userIdx, emitter, eventName, data));
    }

    private void sendEvent(Long userIdx, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            removeEmitter(userIdx, emitter);
        }
    }

    private void removeEmitter(Long userIdx, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(userIdx);
        if (userEmitters == null) {
            return;
        }

        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) {
            emitters.remove(userIdx);
        }
    }
}
