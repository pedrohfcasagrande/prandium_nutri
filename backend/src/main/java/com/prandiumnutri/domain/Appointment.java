package com.prandiumnutri.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

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
    private LocalDateTime scheduledAt;

    @Column(nullable = false)
    @Builder.Default
    private Integer durationMinutes = 60;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AppointmentLocation location = AppointmentLocation.ONLINE;

    @Column(length = 500)
    private String locationDetails;

    /** Fase 9F: URL de videochamada (Google Meet, Zoom, Whereby...). Substitui poluir locationDetails. */
    @Column(length = 500)
    private String meetingUrl;

    /** Fase 9F: marca timestamp quando o job de lembrete disparou pra evitar reenvio. */
    private LocalDateTime reminderSentAt;

    @Column(length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentActor requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentActor lastActionBy;

    private LocalDateTime proposedScheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AppointmentActor proposedBy;

    @Column(length = 500)
    private String cancelReason;

    private LocalDateTime canceledAt;

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
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
