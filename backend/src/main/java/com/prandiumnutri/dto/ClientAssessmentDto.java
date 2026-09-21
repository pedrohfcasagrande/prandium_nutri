package com.prandiumnutri.dto;

import com.prandiumnutri.domain.StressLevel;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClientAssessmentDto(
        Long id,
        Long clientId,
        String clientName,
        String clientEmail,
        Long nutritionistId,
        String nutritionistName,
        Long practiceId,
        LocalDateTime assessmentDate,

        // Antropometria
        Double weightKg,
        Integer heightCm,
        Double bodyFatPct,
        Double waistCm,
        Double hipCm,
        Double neckCm,
        Double armCm,

        // Clinico
        String chiefComplaint,
        String medicalHistory,
        String medications,
        String preExistingConditions,
        String allergies,
        String familyHistory,

        // Habitos
        String dietaryRecall24h,
        String mealFrequency,
        Double waterIntakeLiters,
        String alcoholFrequency,
        String foodPreferences,
        String foodAversions,

        // Estilo
        String physicalActivityDetails,
        Double sleepHours,
        StressLevel stressLevel,
        boolean smoker,
        String smokingDetails,

        // Objetivos
        String goalNotes,
        Double targetWeightKg,
        LocalDate targetDate,

        // Meta
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
