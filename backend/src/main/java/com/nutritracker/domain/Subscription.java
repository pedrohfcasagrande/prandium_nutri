package com.nutritracker.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** COMPANY, NUTRITIONIST ou USER. Discriminador do payer_id. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanTarget payerType;

    /** id de company OU user, dependendo do payer_type. */
    @Column(nullable = false)
    private Long payerId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_code")
    private Plan plan;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private Integer amountCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentProvider provider;

    private String providerSubscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(nullable = false)
    private LocalDateTime currentPeriodStart;

    @Column(nullable = false)
    private LocalDateTime currentPeriodEnd;

    @Column(nullable = false)
    @Builder.Default
    private boolean cancelAtPeriodEnd = false;

    private LocalDateTime canceledAt;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();
}
