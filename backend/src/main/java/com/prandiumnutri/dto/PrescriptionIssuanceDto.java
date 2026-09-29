package com.prandiumnutri.dto;

import java.time.LocalDateTime;

/**
 * Fase 9D-3: registro de emissao de PDF.
 * Usado nas listagens (nutri e cliente).
 */
public record PrescriptionIssuanceDto(
        Long id,
        Long assessmentId,
        Long snapshotId,
        Integer snapshotVersion,   // helper: exibe "consulta v2" na UI
        Long nutriId,
        String nutriName,
        Long clientId,
        String clientName,
        String requesterRole,
        String pdfSha256,
        Long pdfSizeBytes,
        LocalDateTime issuedAt
) {}
