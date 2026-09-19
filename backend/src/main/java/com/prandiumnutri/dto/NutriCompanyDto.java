package com.prandiumnutri.dto;

import com.prandiumnutri.domain.LinkStatus;

import java.time.LocalDateTime;

public record NutriCompanyDto(
        Long linkId,
        Long companyId,
        String companyName,
        LinkStatus status,
        LocalDateTime acceptedAt
) {}
