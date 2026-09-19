package com.prandiumnutri.dto;

import com.prandiumnutri.domain.AuthProvider;
import com.prandiumnutri.domain.Role;

public record AdminUserDto(
        Long id,
        String email,
        /** CPF mascarado (LGPD). */
        String cpf,
        /** CRN quando nutricionista, null caso contrario. */
        String crn,
        String name,
        Role role,
        AuthProvider provider,
        boolean profileCompleted,
        boolean active
) {}
