package com.nutritracker.dto;

import java.time.LocalDate;
import java.util.List;

public record DailySummaryDto(
        LocalDate date,
        Double calorieGoal,
        Double proteinGoal,
        Double caloriesConsumed,
        Double proteinConsumed,
        Double carbsConsumed,
        Double fatConsumed,
        Double calorieRemaining,
        Double proteinRemaining,
        List<MealDto> meals
) {}
