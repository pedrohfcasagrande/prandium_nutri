package com.prandiumnutri.dto;

import com.prandiumnutri.domain.LinkStatus;

import java.time.LocalDate;

/** Cliente + nutri atual dentro de uma empresa. Visao consolidada. */
public record CompanyClientDto(
        Long linkId,
        Long clientId,
        String clientName,
        String clientEmail,
        Long nutriId,
        String nutriName,
        LinkStatus status,
        LocalDate lastMealDate
) {}
