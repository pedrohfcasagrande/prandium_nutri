package com.prandiumnutri.dto;

/**
 * Fase 10A: item estimado pela analise IA de foto de refeicao.
 * Valores por porcao (nao por 100g) — a foto ja mostra a porcao servida.
 */
public record PhotoAnalysisItemDto(
        String name,             // "Arroz branco cozido"
        Double portionGrams,     // estimativa (g)
        Double calories,         // total da porcao
        Double protein,
        Double carbs,
        Double fat,
        Double confidence        // 0..1
) {}
