package org.example.backend.dashboard.dto;

import java.time.LocalDateTime;

public record ReviewQueueItemDto(
        Long taskId,
        String taskName,
        Long campaignId,
        String campaignName,
        Long assigneeId,
        String assigneeName,
        LocalDateTime dueDate,
        String priority
) {}
