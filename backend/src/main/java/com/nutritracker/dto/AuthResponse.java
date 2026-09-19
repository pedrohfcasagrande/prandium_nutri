package com.nutritracker.dto;

import com.nutritracker.domain.Role;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        String name,
        Role role,
        boolean profileCompleted
) {}
