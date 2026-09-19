package com.nutritracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record FoodDto(
        Long id,
        @NotBlank String name,
        String category,
        @NotNull @PositiveOrZero Double caloriesPer100g,
        @NotNull @PositiveOrZero Double proteinPer100g,
        @NotNull @PositiveOrZero Double carbsPer100g,
        @NotNull @PositiveOrZero Double fatPer100g
) {}
