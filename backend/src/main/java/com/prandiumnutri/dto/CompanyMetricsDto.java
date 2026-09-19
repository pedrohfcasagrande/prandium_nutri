package com.prandiumnutri.dto;

import java.util.List;

public class CompanyMetricsDto {

    public record Overview(
            long nutritionistsActive,
            long nutritionistsPending,
            long clientsTotal,
            long aiSuggestionsMonth,
            long mealsLoggedLast30d,
            long activeSubscriptionCents,
            String currency,
            List<NutriRow> nutritionists
    ) {}

    /** Agregado por nutri (SEM PII de cliente). */
    public record NutriRow(
            Long nutriId,
            String name,
            String email,
            long activeClients,
            long aiSuggestionsMonth,
            String lastActivityDate      // ISO date ou null
    ) {}
}
