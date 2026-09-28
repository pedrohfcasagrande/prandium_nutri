package com.prandiumnutri.dto;

import com.prandiumnutri.domain.ConsultationType;

import java.time.LocalDateTime;

public record ConsultationDto(
        Long id,
        Long clientId,
        String clientName,
        Long nutritionistId,
        String nutritionistName,
        Long practiceId,
        LocalDateTime consultationDate,
        ConsultationType type,
        String chiefComplaint,
        String findings,
        String plan,
        LocalDateTime nextConsultationDate,
        Long snapshotId,
        Integer snapshotVersion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
