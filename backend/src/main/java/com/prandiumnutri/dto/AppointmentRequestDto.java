package com.prandiumnutri.dto;

import com.prandiumnutri.domain.AppointmentLocation;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentRequestDto(
        Long nutritionistId,     // usado quando cliente solicita
        Long clientId,           // usado quando nutri sugere
        Long practiceId,         // opcional; usado por nutri
        @NotNull LocalDateTime scheduledAt,
        Integer durationMinutes,
        AppointmentLocation location,
        String locationDetails,
        String meetingUrl,    // Fase 9F
        String notes
) {}
