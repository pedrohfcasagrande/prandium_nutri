package com.prandiumnutri.dto;

import com.prandiumnutri.domain.LinkStatus;

import java.time.LocalDateTime;

public record CompanyNutriLinkDto(
        Long id,
        Long nutritionistId,
        String nutritionistName,
        String nutritionistEmail,
        LinkStatus status,
        LocalDateTime invitedAt,
        LocalDateTime acceptedAt,
        long clientsCount   // # de clientes ativos deste nutri
) {}
