package com.nutritracker.dto;

import com.nutritracker.domain.CompanyMemberRole;
import com.nutritracker.domain.LinkStatus;

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
