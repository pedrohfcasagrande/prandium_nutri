package com.prandiumnutri.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptOnboardingRequest(
        @NotBlank String name,
        @NotBlank @Size(min = 6) String password,
        @AssertTrue(message = "Voce precisa aceitar a politica de coleta de dados") boolean dataConsent
) {}
