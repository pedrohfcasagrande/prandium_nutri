package com.nutritracker.dto;

import com.nutritracker.domain.PaymentProvider;
import com.nutritracker.domain.PlanTarget;
import com.nutritracker.domain.SubscriptionStatus;

import java.time.LocalDateTime;
import java.util.Map;

public record SubscriptionDto(
        Long id,
        PlanTarget payerType,
        Long payerId,
        String planCode,
        String planName,
        String planTier,
        Map<String, Object> features,
        String currency,
        Integer amountCents,
        PaymentProvider provider,
        SubscriptionStatus status,
        LocalDateTime currentPeriodStart,
        LocalDateTime currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        Map<String, Integer> currentMonthUsage
) {}
