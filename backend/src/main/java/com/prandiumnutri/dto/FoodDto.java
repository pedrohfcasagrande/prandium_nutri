package com.prandiumnutri.dto;

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
        @NotNull @PositiveOrZero Double fatPer100g,
        Boolean aiSuggested
) {
    public FoodDto(Long id, String name, String category,
                   Double caloriesPer100g, Double proteinPer100g,
                   Double carbsPer100g, Double fatPer100g) {
        this(id, name, category, caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g, null);
    }
}
