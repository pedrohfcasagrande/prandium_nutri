package com.prandiumnutri.dto;

/**
 * Fase 9H-1: envelope para o GET de template.
 * Frontend usa {@code readOnly} pra desabilitar campos quando o template
 * herdado da empresa (nutri vinculado nao pode editar direto — a partir
 * da 9H-3 ele podera sugerir mudancas via change_requests).
 */
public record PdfTemplateView(
        PdfCustomizationDto template,
        boolean readOnly,
        String inheritedFromCompanyName  // null quando editavel pelo proprio dono
) {}
