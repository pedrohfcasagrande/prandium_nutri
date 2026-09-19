package com.prandiumnutri.dto;

import com.prandiumnutri.domain.PaymentProvider;
import com.prandiumnutri.domain.PlanTarget;

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
