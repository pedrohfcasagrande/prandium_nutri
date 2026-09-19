package com.prandiumnutri.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_fx_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyFxRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 12, scale = 6)
    private BigDecimal rateToBrl;

    @Column(nullable = false)
    private LocalDate rateDate;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String source = "MANUAL";

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
