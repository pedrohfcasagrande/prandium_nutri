package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.dto.PdfVerificationDto;
import com.prandiumnutri.dto.PrescriptionIssuanceDto;
import com.prandiumnutri.repository.PrescriptionIssuanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Fase 9D-3: log imutavel de emissao de PDF de prescricao.
 * Cada render bem-sucedido chama {@link #record} pra registrar hash + metadados;
 * verificacao publica por hash prova autenticidade sem armazenar o PDF.
 */
@Service
@RequiredArgsConstructor
public class PrescriptionIssuanceService {

    private final PrescriptionIssuanceRepository repository;

    @Transactional
    public PrescriptionIssuance record(ClientAssessment assessment, User client, User nutri,
                                        AssessmentSnapshot snapshot, byte[] pdfBytes,
                                        String requesterRole) {
        String hash = sha256Hex(pdfBytes);
        return repository.save(PrescriptionIssuance.builder()
                .assessment(assessment)
                .snapshot(snapshot)
                .nutri(nutri)
                .client(client)
                .requesterRole(requesterRole)
                .pdfSha256(hash)
                .pdfSizeBytes((long) pdfBytes.length)
                .build());
    }

    @Transactional(readOnly = true)
    public List<PrescriptionIssuanceDto> listForAssessment(Long assessmentId) {
        return repository.findByAssessmentIdOrderByIssuedAtDesc(assessmentId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PrescriptionIssuanceDto> listForClient(Long clientId) {
        return repository.findByClientIdOrderByIssuedAtDesc(clientId)
                .stream().map(this::toDto).toList();
    }

    /** Verificacao publica: dado um hash, prova que aquele PDF foi mesmo emitido aqui. */
    @Transactional(readOnly = true)
    public PdfVerificationDto verify(String hash) {
        if (hash == null || hash.length() != 64) return PdfVerificationDto.invalid(hash);
        return repository.findFirstByPdfSha256(hash.toLowerCase())
                .map(PdfVerificationDto::from)
                .orElseGet(() -> PdfVerificationDto.invalid(hash));
    }

    private PrescriptionIssuanceDto toDto(PrescriptionIssuance i) {
        return new PrescriptionIssuanceDto(
                i.getId(),
                i.getAssessment() != null ? i.getAssessment().getId() : null,
                i.getSnapshot() != null ? i.getSnapshot().getId() : null,
                i.getSnapshot() != null ? i.getSnapshot().getVersion() : null,
                i.getNutri() != null ? i.getNutri().getId() : null,
                i.getNutri() != null ? i.getNutri().getName() : null,
                i.getClient() != null ? i.getClient().getId() : null,
                i.getClient() != null ? i.getClient().getName() : null,
                i.getRequesterRole(),
                i.getPdfSha256(),
                i.getPdfSizeBytes(),
                i.getIssuedAt()
        );
    }

    public static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data != null ? data : new byte[0]);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nao disponivel", e);
        }
    }

    /** So pra ergonomia em testes / callers com string. */
    public static String sha256Hex(String s) {
        return sha256Hex(s.getBytes(StandardCharsets.UTF_8));
    }
}
