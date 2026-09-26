package com.prandiumnutri.dto;

import java.time.LocalDate;

/**
 * Campos que o proprio cliente pode preencher na sua ficha, sem intervencao do nutri.
 * Cobre habitos, preferencias, estilo de vida e objetivo — nada clinico
 * (antropometria/labs/prescricao permanecem restritos ao nutri).
 */
public record AssessmentSelfFillDto(
        // Habitos alimentares
        String dietaryRecall24h,
        String mealFrequency,
        Double waterIntakeLiters,
        String alcoholFrequency,
        String foodPreferences,
        String foodAversions,

        // Estilo de vida
        String physicalActivityDetails,
        Double sleepHours,
        Boolean smoker,
        String smokingDetails,

        // Objetivo do proprio cliente
        String goalNotes,
        Double targetWeightKg,
        LocalDate targetDate
) {}
