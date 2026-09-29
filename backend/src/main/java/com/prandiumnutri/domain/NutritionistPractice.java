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

    // ---- Fase 9D-2-fix: customizacao do PDF por practice (movido de users) ----
    @Column(name = "pdf_logo_base64", columnDefinition = "MEDIUMTEXT")
    private String pdfLogoBase64;

    @Column(name = "pdf_primary_color", length = 9)
    private String pdfPrimaryColor;

    @Column(name = "pdf_header_text", length = 500)
    private String pdfHeaderText;

    @Column(name = "pdf_footer_text", length = 1000)
    private String pdfFooterText;

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
