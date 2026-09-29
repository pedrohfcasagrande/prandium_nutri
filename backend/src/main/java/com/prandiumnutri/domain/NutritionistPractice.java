package com.prandiumnutri.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "nutritionist_practices",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_np_nutri_company",
               columnNames = {"nutritionist_id", "company_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NutritionistPractice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nutritionist_id", nullable = false)
    private User nutritionist;

    /** null quando e o perfil particular (sem vinculo de empresa). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(length = 2000)
    private String bio;

    @Column(length = 500)
    private String specialties;

    @Column(name = "accepts_new_clients", nullable = false)
    private boolean acceptsNewClients;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PracticeStatus status = PracticeStatus.ACTIVE;

    // Fase 9H-1 (V034): customizacao do PDF foi movida pra tabela pdf_templates.
    // Practices vinculadas a empresa herdam o template da empresa; particulares tem o seu.

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isParticular() {
        return company == null;
    }
}
