package com.nutritracker.dto;

public record NutriProfileUpdateDto(
        boolean acceptsNewClients,
        String bio,
        String specialties
) {}
