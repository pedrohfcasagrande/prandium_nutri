package com.prandiumnutri.dto;

public record NutriProfileUpdateDto(
        boolean acceptsNewClients,
        String bio,
        String specialties
) {}
