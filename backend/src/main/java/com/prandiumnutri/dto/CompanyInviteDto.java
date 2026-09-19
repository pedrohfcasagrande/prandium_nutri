package com.prandiumnutri.dto;

import java.time.LocalDateTime;

public record CompanyInviteDto(
        Long linkId,
        String token,
        Long companyId,
        String companyName,
        LocalDateTime invitedAt,
        LocalDateTime expiresAt
) {}
