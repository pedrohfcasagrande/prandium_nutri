package com.prandiumnutri.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    /** CPF sem formatacao (11 digitos). LGPD: nunca logar completo; mostrar mascarado via {@link com.prandiumnutri.support.CpfUtil}. */
    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    /** CRN opcional (formato livre, ex "CRN3 12345"). Unico quando presente. */
    @Column(unique = true, length = 30)
    private String crn;

    @Column(nullable = false)
    private String name;

    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    private String providerId;

    @Column(nullable = false)
    @Builder.Default
    private boolean profileCompleted = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    private LocalDateTime consentAcceptedAt;

    @Enumerated(EnumType.STRING)
    private Sex sex;

    private LocalDate birthDate;

    private Double weightKg;

    private Double heightCm;

    @Enumerated(EnumType.STRING)
    private ActivityLevel activityLevel;

    @Enumerated(EnumType.STRING)
    private Goal goal;

    @Column(length = 2000)
    private String preferences;

    @Column(length = 2000)
    private String medications;

    @Column(nullable = false)
    @Builder.Default
    private boolean hasBariatricSurgery = false;

    @Column(length = 2000)
    private String preExistingConditions;

    @Column(length = 2000)
    private String allergies;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastSeenAt;

    // ---- 4D: opt-in de discovery (nutricionistas) ----

    @Column(nullable = false)
    @Builder.Default
    private boolean acceptsNewClients = false;

    @Column(length = 2000)
    private String bio;

    @Column(length = 500)
    private String specialties;

    // ---- Fase 9D: assinatura digital (imagem PNG em data URL base64) ----
    @Column(columnDefinition = "MEDIUMTEXT")
    private String signatureBase64;

    // ---- Fase 9D-2: customizacao do PDF de prescricao ----
    @Column(columnDefinition = "MEDIUMTEXT")
    private String pdfLogoBase64;

    @Column(length = 9)
    private String pdfPrimaryColor;

    @Column(length = 500)
    private String pdfHeaderText;

    @Column(length = 1000)
    private String pdfFooterText;
}
