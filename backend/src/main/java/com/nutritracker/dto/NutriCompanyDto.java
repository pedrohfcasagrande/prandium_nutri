package com.nutritracker.dto;

import com.nutritracker.domain.LinkStatus;

import java.time.LocalDateTime;

public record NutriCompanyDto(
        Long linkId,
        Long companyId,
        String companyName,
        LinkStatus status,
        LocalDateTime acceptedAt
) {}
