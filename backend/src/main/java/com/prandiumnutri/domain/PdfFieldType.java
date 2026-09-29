package com.prandiumnutri.domain;

/**
 * Fase 9H-2: tipo de campo modular no rodape do PDF.
 * Cada tipo controla validacao no frontend e formatacao no renderer.
 */
public enum PdfFieldType {
    TEXT,       // texto curto <= 200 chars
    TEXTAREA,   // texto longo <= 1000 chars, exibido em italico
    URL,        // input tipo url
    EMAIL,      // input tipo email
    TEL,        // input telefone
    INSTAGRAM   // texto; renderizer garante prefixo "@"
}
