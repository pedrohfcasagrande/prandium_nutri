package com.prandiumnutri.dto;

import com.prandiumnutri.domain.ActivityLevel;
import com.prandiumnutri.domain.Goal;
import com.prandiumnutri.domain.Sex;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record HealthProfileDto(
        @NotNull Sex sex,
        @NotNull LocalDate birthDate,
        @NotNull @Positive Double weightKg,
        @NotNull @Positive Double heightCm,
        @NotNull ActivityLevel activityLevel,
        @NotNull Goal goal,
        String preferences,
        String medications,
        Boolean hasBariatricSurgery,
        String preExistingConditions,
        String allergies
) {}
