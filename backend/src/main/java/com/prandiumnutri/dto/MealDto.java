package com.prandiumnutri.dto;

import com.prandiumnutri.domain.MealType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record MealDto(
        Long id,
        Long userId,               // preenchido pelo controller (currentUser)
        @NotNull LocalDate date,
        LocalTime time,
        @NotNull MealType type,
        @NotEmpty @Valid List<MealItemDto> items,
        Double totalCalories,
        Double totalProtein,
        Double totalCarbs,
        Double totalFat
) {}
