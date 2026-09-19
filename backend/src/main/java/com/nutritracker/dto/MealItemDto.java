package com.nutritracker.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MealItemDto(
        Long id,
        @NotNull Long foodId,
        String foodName,
        @NotNull @Positive Double grams,
        Double calories,
        Double protein,
        Double carbs,
        Double fat
) {}
