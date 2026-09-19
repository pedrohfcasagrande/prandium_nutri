package com.nutritracker.support;

import com.nutritracker.domain.PaymentProvider;
import com.nutritracker.domain.Plan;
import com.nutritracker.domain.PlanPrice;
import com.nutritracker.domain.PlanTarget;
import com.nutritracker.repository.PlanPriceRepository;
import com.nutritracker.repository.PlanRepository;

/**
 * Semeia planos e precos para testes. Perfil "test" nao roda Flyway,
 * entao os testes precisam popular os planos programaticamente.
 */
public final class PlanTestFixtures {

    private PlanTestFixtures() {}

    public static void seedAll(PlanRepository planRepo, PlanPriceRepository priceRepo) {
        seedPlan(planRepo, priceRepo, "NUTRI_FREE", PlanTarget.NUTRITIONIST, "FREE", "Free",
                "{\"maxClients\":3,\"aiSuggestionsMonthly\":10,\"menuLimit\":1}",
                0, PaymentProvider.MANUAL);
        seedPlan(planRepo, priceRepo, "NUTRI_PRO", PlanTarget.NUTRITIONIST, "PRO", "Pro",
                "{\"maxClients\":25,\"aiSuggestionsMonthly\":null,\"menuLimit\":null}",
                7900, PaymentProvider.MERCADO_PAGO);
        seedPlan(planRepo, priceRepo, "CLIENT_FREE", PlanTarget.USER, "FREE", "Free",
                "{\"aiSuggestionsMonthly\":3,\"menuLimit\":1,\"findNutritionist\":false}",
                0, PaymentProvider.MANUAL);
        seedPlan(planRepo, priceRepo, "CLIENT_PLUS", PlanTarget.USER, "PLUS", "Plus",
                "{\"aiSuggestionsMonthly\":null,\"menuLimit\":null,\"findNutritionist\":true}",
                1900, PaymentProvider.MERCADO_PAGO);
        seedPlan(planRepo, priceRepo, "COMP_STARTER", PlanTarget.COMPANY, "STARTER", "Starter",
                "{\"maxNutritionists\":3,\"maxClientsPerNutri\":30}",
                49900, PaymentProvider.MERCADO_PAGO);
    }

    public static void seedPlan(PlanRepository planRepo, PlanPriceRepository priceRepo,
                                 String code, PlanTarget target, String tier, String name,
                                 String features, int amountBrlCents, PaymentProvider provider) {
        Plan plan = Plan.builder()
                .code(code).target(target).tier(tier).name(name)
                .features(features).active(true).sortOrder(0)
                .build();
        planRepo.save(plan);

        priceRepo.save(PlanPrice.builder()
                .plan(plan).currency("BRL").amountCents(amountBrlCents)
                .provider(provider).active(true).build());
        priceRepo.save(PlanPrice.builder()
                .plan(plan).currency("USD").amountCents(amountBrlCents / 5)
                .provider(amountBrlCents == 0 ? PaymentProvider.MANUAL : PaymentProvider.STRIPE)
                .active(true).build());
        priceRepo.save(PlanPrice.builder()
                .plan(plan).currency("EUR").amountCents(amountBrlCents / 6)
                .provider(amountBrlCents == 0 ? PaymentProvider.MANUAL : PaymentProvider.STRIPE)
                .active(true).build());
    }
}
