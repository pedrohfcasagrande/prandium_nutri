package com.prandiumnutri.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Fase 9E — Evento formal de atendimento nutri↔cliente.
 * Ao ser criada, opcionalmente pode linkar um {@link AssessmentSnapshot} tirado no momento
 * para preservar o estado da ficha ("foi essa ficha que o cliente tinha na consulta").
 */
@Entity
@Table(name = "consultations",
        indexes = {
                @Index(name = "idx_cons_client_date", columnList = "client_id,consultationDate"),
                @Index(name = "idx_cons_nutri_date",  columnList = "nutritionist_id,consultationDate")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consultation {

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
    private LocalDateTime consultationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsultationType type;

    @Column(columnDefinition = "TEXT") private String chiefComplaint;
    @Column(columnDefinition = "TEXT") private String findings;
    @Column(columnDefinition = "TEXT") private String plan;

    private LocalDateTime nextConsultationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id")
    private AssessmentSnapshot snapshot;

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
