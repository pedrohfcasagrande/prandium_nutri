package com.prandiumnutri.dto;

import com.prandiumnutri.domain.AppointmentActor;
import com.prandiumnutri.domain.AppointmentLocation;
import com.prandiumnutri.domain.AppointmentStatus;

import java.time.LocalDateTime;

public record AppointmentDto(
        Long id,
        Long clientId,
        String clientName,
        String clientEmail,
        Long nutritionistId,
        String nutritionistName,
        Long practiceId,
        Long companyId,
        String companyName,
        LocalDateTime scheduledAt,
        Integer durationMinutes,
        AppointmentLocation location,
        String locationDetails,
        String notes,
        AppointmentStatus status,
        AppointmentActor requestedBy,
        AppointmentActor lastActionBy,
        LocalDateTime proposedScheduledAt,
        AppointmentActor proposedBy,
        String cancelReason,
        LocalDateTime canceledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
