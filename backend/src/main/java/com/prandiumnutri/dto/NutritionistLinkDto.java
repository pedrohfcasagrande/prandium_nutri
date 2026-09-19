package com.prandiumnutri.dto;

import com.prandiumnutri.domain.LinkStatus;

import java.time.LocalDateTime;

public record NutritionistLinkDto(
        Long id,
        Long nutritionistId,
        String nutritionistName,
        String nutritionistEmail,
        LinkStatus status,
        LocalDateTime invitedAt,
        LocalDateTime acceptedAt
) {}
