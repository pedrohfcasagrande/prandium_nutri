package com.nutritracker.dto;

import java.util.List;

public class NutriMetricsDto {

    public record Overview(
            long activeClients,
            long aiSuggestionsMonth,
            long aiSuggestionsLimit,     // -1 = unlimited
            long menusActive,
            double avgAdherence7d,       // 0..1
            long clientsNeedingAttention,
            List<ClientRow> clients
    ) {}

    public record ClientRow(
            Long clientId,
            String name,
            String email,
            double adherence7d,          // 0..1
            String lastMealDate,         // ISO or null
            boolean needsAttention,      // adherence < 0.3
            boolean hasActivePlan
    ) {}
}
