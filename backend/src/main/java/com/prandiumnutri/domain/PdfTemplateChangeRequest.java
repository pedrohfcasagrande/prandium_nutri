package com.prandiumnutri.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Fase 9H-3: solicitacao de mudanca no template PDF de uma empresa.
 * Nutri vinculado (que so pode ler o template) usa isso pra propor alteracoes;
 * qualquer CompanyMember da empresa aprova ou rejeita.
 * Estado proposto e salvo por completo — comparacao "antes vs depois" fica trivial na UI.
 */
@Entity
@Table(name = "pdf_template_change_requests",
       indexes = {
           @Index(name = "idx_pdf_req_tpl_status", columnList = "template_id, status, createdAt"),
           @Index(name = "idx_pdf_req_requester",  columnList = "requested_by, createdAt")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdfTemplateChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private PdfTemplate template;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PdfChangeRequestStatus status = PdfChangeRequestStatus.PENDING;

    @Column(name = "proposed_header_line", length = 200)
    private String proposedHeaderLine;

    @Column(name = "proposed_primary_color", length = 9)
    private String proposedPrimaryColor;

    @Column(name = "proposed_logo_base64", columnDefinition = "MEDIUMTEXT")
    private String proposedLogoBase64;

    /** Lista serializada de fields como JSON (mesmo shape do DTO). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proposed_fields", nullable = false, columnDefinition = "JSON")
    private String proposedFields;

    @Column(length = 1000)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
