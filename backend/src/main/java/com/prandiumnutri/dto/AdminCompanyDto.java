package com.prandiumnutri.dto;

import java.time.LocalDateTime;

public record AdminCompanyDto(
        Long id,
        String name,
        String tradeName,
        String taxId,
        String countryCode,
        String contactEmail,
        String contactPhone,
        boolean active,
        LocalDateTime createdAt,
        Long ownerUserId,
        String ownerName,
        String ownerEmail,
        long activeMembersCount,
        long pendingInvitesCount,
        long nutritionistsCount
) {}
