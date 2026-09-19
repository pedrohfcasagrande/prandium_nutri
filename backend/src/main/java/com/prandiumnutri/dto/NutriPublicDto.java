package com.prandiumnutri.dto;

import com.prandiumnutri.domain.NutritionistPractice;
import com.prandiumnutri.domain.User;

/**
 * Perfil publico do nutricionista para tela de discovery (Fase 5B).
 * Uma linha por practice (particular OU vinculada a uma empresa).
 * Se um nutri tem 2 practices ativas, aparece 2 vezes na busca — uma com companyName preenchido.
 *
 * `id` = practiceId (identifica a "conta de trabalho"), nao o userId.
 * `nutritionistId` = userId (identidade legal do nutri).
 */
public record NutriPublicDto(
        Long id,
        Long nutritionistId,
        String name,
        String bio,
        String specialties,
        Long companyId,
        String companyName
) {
    public static NutriPublicDto from(NutritionistPractice p) {
        User n = p.getNutritionist();
        return new NutriPublicDto(
                p.getId(),
                n.getId(),
                n.getName(),
                p.getBio(),
                p.getSpecialties(),
                p.getCompany() != null ? p.getCompany().getId() : null,
                p.getCompany() != null ? p.getCompany().getName() : null
        );
    }
}
