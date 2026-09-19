package com.nutritracker.dto;

import com.nutritracker.domain.User;

/** Perfil publico do nutricionista para tela de discovery. Sem PII sensivel. */
public record NutriPublicDto(
        Long id,
        String name,
        String bio,
        String specialties
) {
    public static NutriPublicDto from(User u) {
        return new NutriPublicDto(u.getId(), u.getName(), u.getBio(), u.getSpecialties());
    }
}
