package com.prandiumnutri.dto;

import com.prandiumnutri.domain.NutritionistPractice;
import com.prandiumnutri.domain.PracticeStatus;

/**
 * DTO interno do nutri para listar/editar suas practices (Fase 5D).
 * Diferente do NutriPublicDto (que e visao publica no discovery).
 */
public record PracticeDto(
        Long id,
        Long companyId,
        String companyName,
        String bio,
        String specialties,
        boolean acceptsNewClients,
        PracticeStatus status
) {
    public static PracticeDto from(NutritionistPractice p) {
        return new PracticeDto(
                p.getId(),
                p.getCompany() != null ? p.getCompany().getId() : null,
                p.getCompany() != null ? p.getCompany().getName() : null,
                p.getBio(),
                p.getSpecialties(),
                p.isAcceptsNewClients(),
                p.getStatus()
        );
    }
}
