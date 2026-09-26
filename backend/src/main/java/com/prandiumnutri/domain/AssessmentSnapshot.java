package com.prandiumnutri.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Fotografia imutavel de uma {@link ClientAssessment} num momento no tempo.
 * O nutri clica "Nova consulta" na tela do assessment; um snapshot da ficha
 * corrente e criado com incremento de versao. A ficha corrente continua sendo
 * editada normalmente (UPSERT); os snapshots viram timeline read-only.
 */
@Entity
@Table(name = "assessment_snapshots",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_snapshot_assessment_version",
                columnNames = {"assessment_id", "version"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id")
    private ClientAssessment assessment;

    @Column(nullable = false)
    private Integer version;

    @Column(length = 255)
    private String label;

    /** Snapshot completo da ficha em JSON (schema-less para tolerar evolucao). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "JSON")
    private String payload;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
