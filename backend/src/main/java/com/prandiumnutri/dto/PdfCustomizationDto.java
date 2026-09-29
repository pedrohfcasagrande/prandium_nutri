package com.prandiumnutri.dto;

import java.util.List;

/**
 * Fase 9H-2: template PDF com campos modulares.
 * Header e uma linha simples; footer e uma lista ordenada de fields
 * (label + tipo + valor) que o dono do template escolhe.
 */
public record PdfCustomizationDto(
        String pdfLogoBase64,       // data URL PNG/JPG
        String pdfPrimaryColor,     // "#RRGGBB"; null pra default (teal)
        String pdfHeaderLine,       // linha unica de cabecalho
        List<PdfTemplateFieldDto> fields  // ordem visual (0-based); null == lista vazia
) {
    public List<PdfTemplateFieldDto> fields() {
        return fields != null ? fields : List.of();
    }
}
