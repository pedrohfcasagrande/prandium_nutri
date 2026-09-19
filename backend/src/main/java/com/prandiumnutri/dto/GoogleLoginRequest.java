package com.prandiumnutri.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank String idToken,
        Boolean dataConsent
) {}
