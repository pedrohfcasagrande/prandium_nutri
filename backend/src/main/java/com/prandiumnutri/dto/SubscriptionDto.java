package com.prandiumnutri.dto;

import com.prandiumnutri.domain.PaymentProvider;
import com.prandiumnutri.domain.PlanTarget;
import com.prandiumnutri.domain.SubscriptionStatus;

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
