package com.prandiumnutri.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record ProgressEntryDto(
        Long id,
        @NotNull LocalDate date,
        @NotNull @Positive Double weightKg,
        Double bodyFatPercent,
        Double waistCm,
        Double hipCm,
        Double armCm,
        Double chestCm,
        String note
) {}
