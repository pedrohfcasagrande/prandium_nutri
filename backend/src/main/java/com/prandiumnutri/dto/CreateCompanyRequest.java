package com.prandiumnutri.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(
        @NotBlank String name,
        String tradeName,
        String taxId,
        @NotBlank @Size(min = 2, max = 2) String countryCode,
        @NotBlank @Email String contactEmail,
        String contactPhone,
        @NotBlank @Email String ownerEmail
) {}
