package com.prandiumnutri.dto;

import java.time.LocalDateTime;

public record ChatMessageDto(
        Long id,
        Long linkId,
        Long senderId,
        String senderName,
        String content,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {}
