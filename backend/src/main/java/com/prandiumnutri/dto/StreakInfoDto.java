package com.prandiumnutri.dto;

import java.util.List;

/**
 * Fase 10C: motivador leve para o cliente.
 * <ul>
 *   <li>{@code currentStreak}: dias consecutivos com pelo menos 1 refeicao registrada,
 *       terminando hoje ou ontem (se ontem, streak ainda "vivo").</li>
 *   <li>{@code longestStreak}: maior sequencia historica.</li>
 *   <li>{@code badges}: marcos batidos (3d/7d/14d/30d/60d/100d) — baseado em longestStreak.</li>
 *   <li>{@code todayLogged}: cliente ja registrou refeicao hoje? Usado pra mostrar
 *       nudge "nao esquece de registrar antes das 23:59".</li>
 * </ul>
 */
public record StreakInfoDto(
        int currentStreak,
        int longestStreak,
        boolean todayLogged,
        List<String> badges
) {}
