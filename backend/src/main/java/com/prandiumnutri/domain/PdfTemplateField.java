package com.prandiumnutri.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Fase 9H-2: campo modular do rodape do PDF.
 * Um template tem N fields ordenados por {@code position} (0-based).
 */
@Entity
@Table(name = "pdf_template_fields",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_pdf_field_pos",
               columnNames = {"template_id", "position"}),
       indexes = @Index(name = "idx_pdf_field_tpl", columnList = "template_id, position"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdfTemplateField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private PdfTemplate template;

    @Column(nullable = false, length = 100)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 20)
    private PdfFieldType type;

    @Column(name = "field_value", length = 1000)
    private String value;

    @Column(nullable = false)
    private Integer position;

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
