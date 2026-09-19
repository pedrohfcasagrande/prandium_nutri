package com.nutritracker.dto;

import com.nutritracker.domain.PaymentProvider;
import com.nutritracker.domain.PlanTarget;

import java.util.Map;

public record PlanDto(
        String code,
        PlanTarget target,
        String tier,
        String name,
        Map<String, Object> features,
        String currency,
        Integer amountCents,
        PaymentProvider provider
) {}
