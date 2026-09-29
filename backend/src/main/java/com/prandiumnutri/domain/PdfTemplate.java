package com.prandiumnutri.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Fase 9H-1: template do PDF de prescricao pertence a uma empresa (COMPANY) OU
 * a uma practice particular (PRACTICE). Practices vinculadas a empresa herdam o
 * template da empresa — todos os nutris atendendo por aquela marca usam o mesmo layout.
 *
 * A resolucao "qual template usar" fica no {@code PdfTemplateService.resolveEffectiveFor}.
 */
@Entity
@Table(name = "pdf_templates",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_pdf_tpl_owner",
               columnNames = {"owner_type", "owner_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdfTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 20)
    private PdfTemplateOwnerType ownerType;

    /** FK logica: companies.id se COMPANY, nutritionist_practices.id se PRACTICE. */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "pdf_logo_base64", columnDefinition = "MEDIUMTEXT")
    private String pdfLogoBase64;

    @Column(name = "pdf_primary_color", length = 9)
    private String pdfPrimaryColor;

    @Column(name = "pdf_header_line", length = 200)
    private String pdfHeaderLine;

    // Fase 9H-2: campos do rodape agora sao rows em pdf_template_fields (modulares).

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
