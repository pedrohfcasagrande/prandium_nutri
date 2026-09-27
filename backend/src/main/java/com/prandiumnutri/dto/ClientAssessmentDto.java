package com.prandiumnutri.dto;

import com.prandiumnutri.domain.StressLevel;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClientAssessmentDto(
        Long id,
        Long clientId,
        String clientName,
        String clientEmail,
        String clientSex,           // Fase 9B: usado no calculo de %gordura JP-7
        LocalDate clientBirthDate,  // Fase 9B: idade -> formula Jackson-Pollock
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

        // Perimetros adicionais (Fase 9B)
        Double thighCm,
        Double calfCm,
        Double chestCm,
        Double forearmCm,
        Double abdomenCm,

        // Dobras cutaneas Jackson-Pollock (mm) (Fase 9B)
        Double sfTricipital,
        Double sfSubscapular,
        Double sfSuprailiac,
        Double sfAbdominal,
        Double sfThigh,
        Double sfChest,
        Double sfMidaxillary,
        String skinfoldProtocol,

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

        // Exames laboratoriais
        LocalDate labDate,
        Double glucoseFasting,
        Double hba1c,
        Double insulinFasting,
        Double totalCholesterol,
        Double ldl,
        Double hdl,
        Double triglycerides,
        Double tsh,
        Double t4Free,
        Double vitaminD,
        Double vitaminB12,
        Double iron,
        Double ferritin,
        Double crp,
        Double urea,
        Double creatinine,
        Double uricAcid,
        String labNotes,

        // Meta
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
