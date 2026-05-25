package org.example.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.notification.model.dto.SseMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

// Redis Pub/Sub으로 받은 SSE 메시지를 현재 Pod의 SSE 연결에 전달하는 구독자입니다.
@Component
@RequiredArgsConstructor
public class NotificationSseRedisSubscriber implements MessageListener {
    private final ObjectMapper objectMapper;
    private final NotificationSseService sseService;

    // Redis 채널 메시지를 SseMessage로 변환해 현재 Pod의 연결된 사용자에게 전달합니다.
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            SseMessage sseMessage = objectMapper.readValue(payload, SseMessage.class);
            sseService.deliverLocal(sseMessage);
        } catch (Exception ignored) {
            // 잘못된 Pub/Sub 메시지는 알림 저장 흐름을 막지 않기 위해 무시합니다.
        }
    }
}
