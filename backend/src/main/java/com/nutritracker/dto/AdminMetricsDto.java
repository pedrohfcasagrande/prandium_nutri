package com.nutritracker.dto;

import java.util.List;
import java.util.Map;

public class AdminMetricsDto {

    /** Payload principal do overview. Todos os valores em cents (moeda consolidada) ou contagens. */
    public record Overview(
            long totalUsers,
            long totalNutritionists,
            long totalCompanies,
            long activeSubscriptions,
            long dau,
            long wau,
            long mau,
            long newUsers30d,
            long mrrBrlCents,
            Map<String, Long> mrrByCurrency,
            Map<String, Long> planDistribution,
            List<ActionCountDto> suggestionsLast30d,
            List<TopCompanyDto> topCompanies,
            List<TopNutriDto> topNutris
    ) {}

    public record ActionCountDto(String action, long count) {}

    public record TopCompanyDto(Long companyId, String name, long amountCents, String currency) {}

    public record TopNutriDto(Long nutriId, String name, long activeClients) {}
}
