package com.nutritracker.dto;

import com.nutritracker.domain.ActivityLevel;
import com.nutritracker.domain.Goal;
import com.nutritracker.domain.Sex;
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
