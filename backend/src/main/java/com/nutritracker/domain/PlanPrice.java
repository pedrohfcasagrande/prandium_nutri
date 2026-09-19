package com.nutritracker.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plan_prices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_code")
    private Plan plan;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private Integer amountCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentProvider provider;

    private String providerPriceId;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
