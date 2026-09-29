package com.prandiumnutri.dto;

import com.prandiumnutri.domain.PdfFieldType;

/**
 * Fase 9H-2: campo modular do rodape.
 * {@code id} vem nulo quando o cliente inclui um campo novo no PUT;
 * o service compara por id pra decidir CREATE vs UPDATE vs DELETE.
 * {@code position} vem do indice no array serializado.
 */
public record PdfTemplateFieldDto(
        Long id,
        String label,
        PdfFieldType type,
        String value
) {}
