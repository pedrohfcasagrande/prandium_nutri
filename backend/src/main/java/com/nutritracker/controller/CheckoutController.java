package com.nutritracker.controller;

import com.nutritracker.domain.PlanTarget;
import com.nutritracker.domain.Subscription;
import com.nutritracker.domain.User;
import com.nutritracker.payment.CheckoutSessionResult;
import com.nutritracker.payment.PaymentProviderRegistry;
import com.nutritracker.repository.CompanyRepository;
import com.nutritracker.security.CurrentUser;
import com.nutritracker.service.CheckoutService;
import com.nutritracker.service.SubscriptionService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final CurrentUser currentUser;
    private final SubscriptionService subscriptionService;
    private final PaymentProviderRegistry providerRegistry;
    private final CompanyRepository companyRepo;

    /** Cria sessao de checkout para plano/currency escolhido. Retorna URL para redirecionar. */
    @PostMapping
    public Map<String, String> create(@RequestBody CheckoutRequest req) {
        User u = currentUser.getAuthenticated();
        CheckoutSessionResult r = checkoutService.startCheckout(u, req.planCode(), req.currency());
        return Map.of("checkoutUrl", r.checkoutUrl(), "sessionId", r.providerSessionId());
    }

    /** Cancela a subscription atual do usuario (no fim do periodo). */
    @PostMapping("/cancel")
    public Map<String, String> cancel(@RequestParam(defaultValue = "true") boolean atPeriodEnd) {
        User u = currentUser.getAuthenticated();
        PlanTarget target = resolveTarget(u);
        Long payerId = resolvePayerId(u, target);
        if (payerId == null) throw new IllegalArgumentException("Nao ha subscription para cancelar");

        Subscription sub = subscriptionService.effectiveOf(target, payerId)
                .orElseThrow(() -> new IllegalArgumentException("Sem subscription ativa"));

        if (sub.getProvider() == com.nutritracker.domain.PaymentProvider.MANUAL) {
            // Free plan: nao ha o que cancelar no provider
            return Map.of("message", "Plano gratuito nao requer cancelamento.");
        }

        providerRegistry.forProvider(sub.getProvider()).cancelSubscription(sub, atPeriodEnd);
        sub.setCancelAtPeriodEnd(atPeriodEnd);
        return Map.of("message", "Cancelamento solicitado. Confirmacao vira via webhook.");
    }

    private PlanTarget resolveTarget(User u) {
        return switch (u.getRole()) {
            case USER -> PlanTarget.USER;
            case NUTRITIONIST -> PlanTarget.NUTRITIONIST;
            case COMPANY -> PlanTarget.COMPANY;
            case ADMIN -> throw new IllegalArgumentException("Admin nao possui plano");
        };
    }

    private Long resolvePayerId(User u, PlanTarget target) {
        return switch (target) {
            case COMPANY -> companyRepo.findByOwnerId(u.getId()).map(c -> c.getId()).orElse(null);
            case NUTRITIONIST, USER -> u.getId();
        };
    }

    public record CheckoutRequest(@NotBlank String planCode, @NotBlank String currency) {}
}
