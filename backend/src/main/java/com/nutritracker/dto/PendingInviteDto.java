package com.nutritracker.dto;

import java.time.LocalDateTime;

public record PendingInviteDto(
        Long linkId,
        String token,
        Long nutritionistId,
        String nutritionistName,
        String nutritionistEmail,
        LocalDateTime invitedAt,
        LocalDateTime expiresAt
) {}
