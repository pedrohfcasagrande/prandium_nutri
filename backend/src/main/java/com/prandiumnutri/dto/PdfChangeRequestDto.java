package com.prandiumnutri.dto;

import com.prandiumnutri.domain.PdfChangeRequestStatus;

import java.time.LocalDateTime;

/**
 * Fase 9H-3: leitura de solicitacoes de mudanca no PDF.
 * `currentTemplate` e o estado atual do template (pra montar diff "antes vs depois" na UI).
 */
public record PdfChangeRequestDto(
        Long id,
        Long templateId,
        Long requestedById,
        String requestedByName,
        PdfChangeRequestStatus status,

        // Estado proposto
        PdfCustomizationDto proposed,

        String note,

        Long reviewedById,
        String reviewedByName,
        LocalDateTime reviewedAt,
        String reviewNote,

        LocalDateTime createdAt
) {}
