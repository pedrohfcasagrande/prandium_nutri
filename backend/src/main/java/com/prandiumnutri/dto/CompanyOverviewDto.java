package com.prandiumnutri.dto;

public record CompanyOverviewDto(
        long nutritionistsActive,
        long nutritionistsPending,
        long clientsTotal
) {}
