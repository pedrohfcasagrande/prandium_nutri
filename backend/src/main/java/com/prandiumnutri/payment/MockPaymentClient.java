package com.prandiumnutri.payment;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.repository.CompanyRepository;
import com.prandiumnutri.repository.SubscriptionRepository;
import com.prandiumnutri.service.PricingService;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mock de provider de pagamento (usado em dev/test).
 * Ativa a Subscription IMEDIATAMENTE no createCheckout, sem redirect real.
 * Retorna URL que leva direto pra tela de sucesso do frontend.
 */
@Slf4j
public class MockPaymentClient implements PaymentProviderClient {

    private final PaymentProvider provider;
    private final SubscriptionRepository subRepo;
    private final PricingService pricingService;
    private final CompanyRepository companyRepo;

    public MockPaymentClient(PaymentProvider provider,
                             SubscriptionRepository subRepo,
                             PricingService pricingService,
                             CompanyRepository companyRepo) {
        this.provider = provider;
        this.subRepo = subRepo;
        this.pricingService = pricingService;
        this.companyRepo = companyRepo;
    }

    @Override
    public PaymentProvider provider() { return provider; }

    @Override
    public boolean isConfigured() { return true; }

    @Override
    public CheckoutSessionResult createCheckout(User payer, Plan plan, PlanPrice price,
                                                 String successUrl, String cancelUrl) {
        PlanTarget target = plan.getTarget();
        Long payerId = resolvePayerId(payer, target);
        if (payerId == null) {
            throw new IllegalArgumentException("Nao foi possivel resolver payer para plano " + plan.getCode());
        }
        cancelExistingActive(target, payerId);

        String fakeSubscriptionId = "mock_sub_" + System.currentTimeMillis();
        Subscription sub = Subscription.builder()
                .payerType(target)
                .payerId(payerId)
                .plan(plan)
                .currency(price.getCurrency())
                .amountCents(price.getAmountCents())
                .provider(provider)
                .providerSubscriptionId(fakeSubscriptionId)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
                .build();
        subRepo.save(sub);

        log.info("[MOCK PAYMENT] Subscription {} ativada instantaneamente para user {} (plan {}, provider {})",
                fakeSubscriptionId, payer.getId(), plan.getCode(), provider);

        String url = successUrl + (successUrl.contains("?") ? "&" : "?") + "mock=true";
        return new CheckoutSessionResult(url, fakeSubscriptionId);
    }

    @Override
    public void cancelSubscription(Subscription sub, boolean atPeriodEnd) {
        log.info("[MOCK PAYMENT] Cancelamento simulado da sub {} (atPeriodEnd={})",
                sub.getProviderSubscriptionId(), atPeriodEnd);
        if (atPeriodEnd) {
            sub.setCancelAtPeriodEnd(true);
            subRepo.save(sub);
        } else {
            sub.setStatus(SubscriptionStatus.CANCELED);
            sub.setCanceledAt(LocalDateTime.now());
            subRepo.save(sub);
        }
    }

    private Long resolvePayerId(User user, PlanTarget target) {
        return switch (target) {
            case COMPANY -> companyRepo.findByOwnerId(user.getId()).map(Company::getId).orElse(null);
            case NUTRITIONIST, USER -> user.getId();
        };
    }

    private void cancelExistingActive(PlanTarget target, Long payerId) {
        List<Subscription> subs = subRepo.findByPayerTypeAndPayerIdOrderByStartedAtDesc(target, payerId);
        for (Subscription s : subs) {
            if (s.getStatus() == SubscriptionStatus.ACTIVE
                    || s.getStatus() == SubscriptionStatus.TRIALING) {
                s.setStatus(SubscriptionStatus.CANCELED);
                s.setCanceledAt(LocalDateTime.now());
                subRepo.save(s);
            }
        }
    }
}
