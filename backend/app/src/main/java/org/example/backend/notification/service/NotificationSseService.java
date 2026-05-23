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

    /**
     * 캘린더 데이터 변경을 모든 SSE 구독자에게 broadcast.
     * 클라이언트는 calendar.refresh 이벤트를 받으면 본인 캘린더 데이터를 다시 로드.
     * payload: { campaignIdx, kind } — kind: "campaign" | "milestone" | "deadline" | "task"
     */
    public void broadcastCalendarRefresh(Long campaignIdx, String kind) {
        Map<String, Object> payload = Map.of(
                "campaignIdx", campaignIdx == null ? -1 : campaignIdx,
                "kind", kind == null ? "unknown" : kind
        );
        emitters.forEach((userIdx, userEmitters) ->
                userEmitters.forEach(emitter -> sendEvent(userIdx, emitter, "calendar.refresh", payload)));
    }

    /**
     * 특정 사용자의 "내 캠페인" 목록 변경 알림 (멤버 추가/추방/초대 수락).
     * Sidebar2 / OverView가 my-campaigns.refresh 이벤트를 받으면 본인 캠페인 목록 재로드.
     */
    public void notifyMyCampaignsRefresh(Long userIdx) {
        if (userIdx == null) return;
        sendToUser(userIdx, "my-campaigns.refresh", Map.of("ts", System.currentTimeMillis()));
    }

    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        emitters.forEach((userIdx, userEmitters) ->
                userEmitters.forEach(emitter -> sendEvent(userIdx, emitter, "heartbeat", "ping")));
    }

    public void sendToUser(Long userIdx, String eventName, Object data) {
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
