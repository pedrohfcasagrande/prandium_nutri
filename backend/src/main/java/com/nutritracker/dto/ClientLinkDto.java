package com.nutritracker.dto;

import com.nutritracker.domain.LinkStatus;

import java.time.LocalDateTime;

public record ClientLinkDto(
        Long id,
        Long clientId,
        String clientName,
        String clientEmail,
        LinkStatus status,
        LocalDateTime invitedAt,
        LocalDateTime acceptedAt,
        LocalDateTime revokedAt
) {}
