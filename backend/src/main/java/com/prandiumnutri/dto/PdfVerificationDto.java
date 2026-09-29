package com.prandiumnutri.dto;

import com.prandiumnutri.support.CpfUtil;

import java.time.LocalDateTime;

/**
 * Fase 9D-3: resposta da verificacao publica por hash SHA-256.
 * NAO expoe dados pessoais do cliente — so nome do nutri + iniciais do cliente,
 * pra permitir prova de autenticidade sem violar privacidade.
 */
public record PdfVerificationDto(
        boolean valid,
        String pdfSha256,
        LocalDateTime issuedAt,
        String nutriName,
        String nutriCrn,
        String clientInitials,    // "J. S." — so iniciais
        String clientCpfMasked    // "***.***.***-45"
) {
    public static PdfVerificationDto invalid(String hash) {
        return new PdfVerificationDto(false, hash, null, null, null, null, null);
    }

    public static PdfVerificationDto from(com.prandiumnutri.domain.PrescriptionIssuance i) {
        return new PdfVerificationDto(
                true,
                i.getPdfSha256(),
                i.getIssuedAt(),
                i.getNutri() != null ? i.getNutri().getName() : null,
                i.getNutri() != null ? i.getNutri().getCrn() : null,
                initials(i.getClient() != null ? i.getClient().getName() : null),
                i.getClient() != null ? CpfUtil.mask(i.getClient().getCpf()) : null
        );
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) return null;
        String[] parts = name.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length && i < 3; i++) {
            if (!parts[i].isBlank()) {
                out.append(Character.toUpperCase(parts[i].charAt(0))).append(". ");
            }
        }
        return out.toString().trim();
    }
}
