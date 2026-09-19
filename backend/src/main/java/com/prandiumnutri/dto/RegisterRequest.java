package com.prandiumnutri.dto;

import com.prandiumnutri.domain.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, message = "Senha deve ter ao menos 6 caracteres") String password,
        @AssertTrue(message = "Voce precisa aceitar a politica de coleta de dados") boolean dataConsent,
        Role role
) {}
