package com.nutritracker.dto;

import com.nutritracker.domain.MenuPlanSource;
import com.nutritracker.domain.MenuPlanStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MenuPlanDto(
        Long id,
        @NotBlank String name,
        @NotNull MenuPlanSource source,
        @NotBlank String content,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        LocalDateTime createdAt,
        MenuPlanStatus status,
        Long submittedToNutriId,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        String reviewNote,
        // Info do cliente (usado pelo nutri quando lista pendentes)
        Long ownerUserId,
        String ownerName,
        String ownerEmail
) {}
