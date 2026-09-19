package com.prandiumnutri.dto;

import com.prandiumnutri.domain.AuthProvider;
import com.prandiumnutri.domain.Role;

public record AdminUserDto(
        Long id,
        String email,
        String name,
        Role role,
        AuthProvider provider,
        boolean profileCompleted,
        boolean active
) {}
