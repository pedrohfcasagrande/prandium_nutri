package com.prandiumnutri.dto;

import java.time.LocalDate;
import java.util.List;

public record DailySummaryDto(
        LocalDate date,
        Double calorieGoal,
        Double proteinGoal,
        Double carbsGoal,
        Double fatGoal,
        Double waterGoalMl,
        Double caloriesConsumed,
        Double proteinConsumed,
        Double carbsConsumed,
        Double fatConsumed,
        Double calorieRemaining,
        Double proteinRemaining,
        /** Hidratacao consumida no dia (soma dos {@code water_intakes} da data). Null se nao ha registros. */
        Integer waterConsumedMl,
        /** "PRESCRIPTION" quando as metas vem da ficha do nutri, "SELF" quando calculado do proprio user, null quando indefinido. */
        String goalsSource,
        List<MealDto> meals
) {}
