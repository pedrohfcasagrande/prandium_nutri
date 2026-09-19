package com.nutritracker.dto;

import java.util.List;

public record PricingDto(
        String currency,
        List<PlanDto> plans
) {}
