package com.nutritracker.dto;

public record CompanyOverviewDto(
        long nutritionistsActive,
        long nutritionistsPending,
        long clientsTotal
) {}
