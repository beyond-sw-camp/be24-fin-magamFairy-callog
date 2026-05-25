package org.example.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.notification.model.dto.NotificationDto;
import org.example.notification.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 사용자 알림 목록과 읽지 않은 알림 수를 조회하는 읽기 전용 서비스입니다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {
    private final NotificationRepository notificationRepository;

    // 최신 알림 목록과 읽지 않은 알림 개수를 함께 조회합니다.
    public NotificationDto.ListRes list(Long userId, Integer count) {
        int size = count == null ? 20 : Math.min(Math.max(count, 1), 100);

        var notifications = notificationRepository
                .findAllByRecipientUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, size))
                .stream()
                .map(NotificationDto.Res::from)
                .toList();

        long unreadCount = notificationRepository.countByRecipientUserIdAndIsReadFalse(userId);
        return new NotificationDto.ListRes(notifications, unreadCount);
    }
}
