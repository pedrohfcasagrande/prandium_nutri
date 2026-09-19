package com.nutritracker.dto;

import com.nutritracker.domain.Notification;
import com.nutritracker.domain.NotificationType;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        NotificationType type,
        String title,
        String body,
        String entityType,
        Long entityId,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(n.getId(), n.getType(), n.getTitle(), n.getBody(),
                n.getEntityType(), n.getEntityId(), n.getReadAt(), n.getCreatedAt());
    }
}
