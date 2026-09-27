package com.prandiumnutri.dto;

/** Fase 9D-2: customizacao do PDF de prescricao por nutri. */
public record PdfCustomizationDto(
        String pdfLogoBase64,      // data URL PNG/JPG; null pra remover
        String pdfPrimaryColor,    // "#RRGGBB" ou "#RRGGBBAA"; null pra default (teal)
        String pdfHeaderText,      // HTML sanitizado exibido no header
        String pdfFooterText       // HTML sanitizado exibido no rodape
) {}
