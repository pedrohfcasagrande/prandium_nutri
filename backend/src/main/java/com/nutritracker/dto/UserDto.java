package com.nutritracker.dto;

import com.nutritracker.domain.ActivityLevel;
import com.nutritracker.domain.Goal;
import com.nutritracker.domain.Sex;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record UserDto(
        Long id,
        @NotBlank @Email String email,
        @NotBlank String name,
        Sex sex,
        LocalDate birthDate,
        Double weightKg,
        Double heightCm,
        ActivityLevel activityLevel,
        Goal goal,
        String preferences,
        String medications,
        Boolean hasBariatricSurgery,
        String preExistingConditions,
        String allergies
) {}
