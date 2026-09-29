package com.prandiumnutri.dto;

/**
 * Fase 9D-2: customizacao do PDF de prescricao por practice.
 * V033: cabecalho/rodape passam a ser campos estruturados (sem HTML) —
 * nutricionista nao precisa saber marcacao pra montar seu proprio template.
 */
public record PdfCustomizationDto(
        String pdfLogoBase64,       // data URL PNG/JPG; null pra remover
        String pdfPrimaryColor,     // "#RRGGBB" ou "#RRGGBBAA"; null pra default (teal)

        // Cabecalho — 1 linha simples
        String pdfHeaderLine,

        // Rodape — 5 campos de contato + 1 nota livre
        String pdfFooterAddress,
        String pdfFooterPhone,
        String pdfFooterEmail,
        String pdfFooterWebsite,
        String pdfFooterInstagram,
        String pdfFooterNote
) {}
