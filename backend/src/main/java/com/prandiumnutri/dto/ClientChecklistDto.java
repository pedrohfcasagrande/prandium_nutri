package com.prandiumnutri.dto;

/**
 * Fase 10D: onboarding do cliente.
 * <p>Cada flag e derivada do estado atual — nao persistimos o "concluido".
 * Se o cliente registrar 1 refeicao e depois deletar, o passo volta a incompleto.
 * Simplifica: sem migration, sem "ja vi essa dica" pra manter.
 */
public record ClientChecklistDto(
        boolean profileFilled,          // healthProfile completo (peso/altura/objetivo)
        boolean goalsPrescribed,        // ficha do nutri com targetCalories
        boolean firstMealLogged,        // >=1 meal registrada
        boolean firstProgressLogged,    // >=1 progress entry
        int completedSteps,
        int totalSteps
) {}
