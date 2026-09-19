package com.prandiumnutri.dto;

import com.prandiumnutri.domain.CompanyMemberRole;
import com.prandiumnutri.domain.LinkStatus;

import java.time.LocalDateTime;

public record CompanyMemberDto(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        CompanyMemberRole roleInCompany,
        LinkStatus status,
        LocalDateTime invitedAt,
        LocalDateTime acceptedAt
) {}
