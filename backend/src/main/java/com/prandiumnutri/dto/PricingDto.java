package com.prandiumnutri.dto;

import java.util.List;

public record PricingDto(
        String currency,
        List<PlanDto> plans
) {}
