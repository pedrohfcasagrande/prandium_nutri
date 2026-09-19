package com.nutritracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteClientRequest(
        @NotBlank @Email String clientEmail,
        String message
) {}
