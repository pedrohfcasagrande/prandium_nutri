package com.nutritracker.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owner primario (nullable ate primeiro admin aceitar convite). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", unique = true)
    private User owner;

    /** Admin da plataforma que cadastrou a empresa (audit). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_id")
    private User createdByAdmin;

    @Column(nullable = false)
    private String name;

    private String tradeName;

    private String taxId;

    @Column(nullable = false)
    @Builder.Default
    private String countryCode = "BR";

    @Column(nullable = false)
    private String contactEmail;

    private String contactPhone;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
