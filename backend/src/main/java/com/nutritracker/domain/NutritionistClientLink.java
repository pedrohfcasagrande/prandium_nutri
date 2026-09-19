package com.nutritracker.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "nutritionist_clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NutritionistClientLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nutritionist_id")
    private User nutritionist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private User client;

    @Column(nullable = false)
    private String invitedByEmail;

    @Column(nullable = false, unique = true)
    private String inviteToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LinkStatus status;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime invitedAt = LocalDateTime.now();

    private LocalDateTime acceptedAt;
    private LocalDateTime revokedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean initiatedByClient = false;

    @Column(length = 1000)
    private String requestMessage;

    @Column(length = 1000)
    private String rejectNote;

    public boolean isActive() { return status == LinkStatus.ACTIVE; }

    public boolean isPending() {
        return status == LinkStatus.PENDING && LocalDateTime.now().isBefore(expiresAt);
    }
}
