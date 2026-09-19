package com.prandiumnutri.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanTarget target;

    @Column(nullable = false)
    private String tier;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "JSON")
    private String features;   // JSON string; parseado no service

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
