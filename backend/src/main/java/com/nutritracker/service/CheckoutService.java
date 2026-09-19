package com.nutritracker.service;

import com.nutritracker.domain.*;
import com.nutritracker.payment.CheckoutSessionResult;
import com.nutritracker.payment.PaymentProviderRegistry;
import com.nutritracker.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final PricingService pricingService;
    private final PaymentProviderRegistry providerRegistry;
    private final CompanyRepository companyRepository;

    @Value("${nutritracker.checkout.success-url:http://localhost:4200/billing?checkout=success}")
    private String successUrl;

    @Value("${nutritracker.checkout.cancel-url:http://localhost:4200/pricing?checkout=canceled}")
    private String cancelUrl;

    /**
     * Cria uma sessao de checkout para o user assinar um plano.
     * Currency detectada do preco cadastrado do plano naquela moeda.
     */
    @Transactional
    public CheckoutSessionResult startCheckout(User user, String planCode, String currency) {
        Plan plan = pricingService.getPlanOrThrow(planCode);
        PlanPrice price = pricingService.getPriceOrThrow(planCode, currency);

        if (price.getAmountCents() == 0) {
            throw new IllegalArgumentException("Este plano e gratuito e nao requer checkout");
        }
        // Validar target vs role
        validateTarget(user, plan);

        var client = providerRegistry.forProvider(price.getProvider());
        if (!client.isConfigured()) {
            throw new IllegalStateException(
                    "Provedor " + price.getProvider() + " nao esta configurado no servidor.");
        }

        return client.createCheckout(user, plan, price, successUrl, cancelUrl);
    }

    private void validateTarget(User user, Plan plan) {
        switch (plan.getTarget()) {
            case USER -> { if (user.getRole() != Role.USER) badTarget(); }
            case NUTRITIONIST -> { if (user.getRole() != Role.NUTRITIONIST) badTarget(); }
            case COMPANY -> {
                if (user.getRole() != Role.COMPANY) badTarget();
                if (companyRepository.findByOwnerId(user.getId()).isEmpty()) {
                    throw new IllegalArgumentException("Voce precisa ser owner de uma empresa para assinar planos COMPANY");
                }
            }
        }
    }
    private void badTarget() {
        throw new IllegalArgumentException("Este plano nao esta disponivel para o seu tipo de conta");
    }
}
