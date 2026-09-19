package com.nutritracker.dto;

import com.nutritracker.domain.AuthProvider;
import com.nutritracker.domain.Role;

public record AdminUserDto(
        Long id,
        String email,
        String name,
        Role role,
        AuthProvider provider,
        boolean profileCompleted,
        boolean active
) {}
