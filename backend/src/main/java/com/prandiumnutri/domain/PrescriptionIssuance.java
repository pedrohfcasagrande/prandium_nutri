package com.prandiumnutri.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Fase 9D-3: log imutavel de emissao do PDF de prescricao.
 * Cada render gera uma row aqui com hash SHA-256 do binario — permite
 * verificacao publica de autenticidade sem armazenar o PDF em si.
 */
@Entity
@Table(name = "prescription_issuances",
       indexes = {
           @Index(name = "idx_issuance_assessment", columnList = "assessment_id, issuedAt"),
           @Index(name = "idx_issuance_client",     columnList = "client_id, issuedAt"),
           @Index(name = "idx_issuance_hash",       columnList = "pdfSha256")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionIssuance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private ClientAssessment assessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id")
    private AssessmentSnapshot snapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nutri_id", nullable = false)
    private User nutri;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @Column(name = "requester_role", nullable = false, length = 20)
    private String requesterRole;   // 'NUTRITIONIST' ou 'CLIENT'

    @Column(name = "pdf_sha256", nullable = false, columnDefinition = "CHAR(64)")
    private String pdfSha256;

    @Column(name = "pdf_size_bytes", nullable = false)
    private Long pdfSizeBytes;

    @Column(name = "issued_at", nullable = false)
    @Builder.Default
    private LocalDateTime issuedAt = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        if (issuedAt == null) issuedAt = LocalDateTime.now();
    }
}
