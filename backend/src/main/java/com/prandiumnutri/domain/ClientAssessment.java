package com.prandiumnutri.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "client_assessments",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_assessment_nutri_client",
                columnNames = {"nutritionist_id", "client_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id")
    private User client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nutritionist_id")
    private User nutritionist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_id")
    private NutritionistPractice practice;

    @Column(nullable = false)
    private LocalDateTime assessmentDate;

    // ----- Antropometria -----
    private Double weightKg;
    private Integer heightCm;
    private Double bodyFatPct;
    private Double waistCm;
    private Double hipCm;
    private Double neckCm;
    private Double armCm;

    // ----- Perimetros adicionais (Fase 9B) -----
    private Double thighCm;
    private Double calfCm;
    private Double chestCm;
    private Double forearmCm;
    private Double abdomenCm;

    // ----- Dobras cutaneas Jackson-Pollock (mm) (Fase 9B) -----
    @Column(name = "sf_tricipital")   private Double sfTricipital;
    @Column(name = "sf_subscapular")  private Double sfSubscapular;
    @Column(name = "sf_suprailiac")   private Double sfSuprailiac;
    @Column(name = "sf_abdominal")    private Double sfAbdominal;
    @Column(name = "sf_thigh")        private Double sfThigh;
    @Column(name = "sf_chest")        private Double sfChest;
    @Column(name = "sf_midaxillary")  private Double sfMidaxillary;

    @Column(length = 30) private String skinfoldProtocol;

    // ----- Perfil clinico -----
    @Column(columnDefinition = "TEXT") private String chiefComplaint;
    @Column(columnDefinition = "TEXT") private String medicalHistory;
    @Column(columnDefinition = "TEXT") private String medications;
    @Column(columnDefinition = "TEXT") private String preExistingConditions;
    @Column(columnDefinition = "TEXT") private String allergies;
    @Column(columnDefinition = "TEXT") private String familyHistory;

    // ----- Habitos alimentares -----
    @Column(name = "dietary_recall_24h", columnDefinition = "TEXT") private String dietaryRecall24h;
    @Column(length = 500)  private String mealFrequency;
    private Double waterIntakeLiters;
    @Column(length = 500)  private String alcoholFrequency;
    @Column(columnDefinition = "TEXT") private String foodPreferences;
    @Column(columnDefinition = "TEXT") private String foodAversions;

    // ----- Estilo de vida -----
    @Column(columnDefinition = "TEXT") private String physicalActivityDetails;
    private Double sleepHours;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StressLevel stressLevel;

    @Column(nullable = false)
    @Builder.Default
    private boolean smoker = false;

    @Column(length = 500) private String smokingDetails;

    // ----- Objetivos -----
    @Column(columnDefinition = "TEXT") private String goalNotes;
    private Double targetWeightKg;
    private LocalDate targetDate;

    // ----- Prescricao energetica + macros (Fase 9C) -----
    @Column(length = 30) private String tmbFormula;    // MIFFLIN / HARRIS_BENEDICT / KATCH_MCARDLE
    private Double activityFactor;                     // override manual
    private Double targetCalories;
    private Double targetProteinG;
    private Double targetCarbsG;
    private Double targetFatG;
    private Double targetWaterMl;
    private Double proteinGPerKg;
    private Double fatPct;
    private Double calorieDeltaPct;
    private LocalDateTime prescribedAt;

    // ----- Exames laboratoriais -----
    private LocalDate labDate;
    private Double glucoseFasting;
    private Double hba1c;
    private Double insulinFasting;
    private Double totalCholesterol;
    private Double ldl;
    private Double hdl;
    private Double triglycerides;
    private Double tsh;
    @Column(name = "t4_free") private Double t4Free;
    private Double vitaminD;
    private Double vitaminB12;
    private Double iron;
    private Double ferritin;
    private Double crp;
    private Double urea;
    private Double creatinine;
    private Double uricAcid;
    @Column(columnDefinition = "TEXT") private String labNotes;

    // ----- Meta -----
    @Column(columnDefinition = "TEXT") private String notes;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
