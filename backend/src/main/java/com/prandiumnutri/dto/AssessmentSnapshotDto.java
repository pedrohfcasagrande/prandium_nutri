package com.prandiumnutri.dto;

import java.time.LocalDateTime;

/** Listagem/leitura de snapshots. `payload` e o DTO completo do assessment como veio no momento do snapshot. */
public record AssessmentSnapshotDto(
        Long id,
        Long assessmentId,
        Integer version,
        String label,
        Long createdById,
        String createdByName,
        LocalDateTime createdAt,
        ClientAssessmentDto payload
) {}
