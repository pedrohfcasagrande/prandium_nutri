package com.prandiumnutri.dto;

import com.prandiumnutri.domain.Role;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        Long userId,
        String email,
        String name,
        Role role,
        boolean profileCompleted
) {}
