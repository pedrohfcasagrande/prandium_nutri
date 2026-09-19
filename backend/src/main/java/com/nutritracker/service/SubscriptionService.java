package com.nutritracker.service;

import com.nutritracker.domain.*;
import com.nutritracker.dto.SubscriptionDto;

import com.nutritracker.repository.SubscriptionRepository;
import com.nutritracker.repository.SubscriptionUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionUsageRepository usageRepository;
    private final PricingService pricingService;
    private final AuditService auditService;

    /**
     * Cria uma subscription FREE/default (MANUAL) para o payer.
     * Usado no signup automatico.
     */
    @Transactional
    public Subscription createFreeSubscription(PlanTarget payerType, Long payerId, String defaultCurrency) {
        String planCode = switch (payerType) {
            case COMPANY -> "COMP_STARTER";        // empresa nao tem FREE; comeca em Starter (mas MANUAL, sem cobranca)
            case NUTRITIONIST -> "NUTRI_FREE";
            case USER -> "CLIENT_FREE";
        };
        Plan plan = pricingService.getPlanOrThrow(planCode);
        PlanPrice price;
        try {
            price = pricingService.getPriceOrThrow(planCode, defaultCurrency);
        } catch (Exception e) {
            // fallback USD
            price = pricingService.getPriceOrThrow(planCode, "USD");
        }
        Subscription sub = Subscription.builder()
                .payerType(payerType)
                .payerId(payerId)
                .plan(plan)
                .currency(price.getCurrency())
                .amountCents(price.getAmountCents())
                .provider(PaymentProvider.MANUAL)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(LocalDateTime.now().plusYears(100)) // FREE nao expira
                .build();
        Subscription saved = subscriptionRepository.save(sub);
        Role actorRole = switch (payerType) {
            case NUTRITIONIST -> Role.NUTRITIONIST;
            case COMPANY -> Role.ADMIN;
            case USER -> Role.USER;
        };
        auditService.record(AuditAction.SUBSCRIPTION_CREATED, payerId, actorRole,
                "Subscription", saved.getId());
        return saved;
    }

    /** Retorna a subscription "efetiva" (ACTIVE ou TRIALING) do payer. Pode ser vazio. */
    @Transactional(readOnly = true)
    public Optional<Subscription> effectiveOf(PlanTarget payerType, Long payerId) {
        return subscriptionRepository.findFirstByPayerTypeAndPayerIdAndStatusInOrderByStartedAtDesc(
                payerType, payerId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING));
    }

    /** Garante que existe uma subscription; se nao existir, cria FREE. */
    @Transactional
    public Subscription ensureSubscription(PlanTarget payerType, Long payerId, String currency) {
        return effectiveOf(payerType, payerId)
                .orElseGet(() -> createFreeSubscription(payerType, payerId,
                        currency == null ? "USD" : currency));
    }

    @Transactional(readOnly = true)
    public SubscriptionDto toDto(Subscription s) {
        String yyyymm = YearMonth.now().toString();
        Map<String, Integer> usage = new HashMap<>();
        // Nao ha metodo pra listar todas as usages; pega principais keys
        for (String key : List.of("aiSuggestionsMonthly")) {
            usageRepository.findBySubscriptionIdAndFeatureKeyAndPeriodYyyymm(s.getId(), key, yyyymm)
                    .ifPresent(u -> usage.put(key, u.getCount()));
        }
        return new SubscriptionDto(
                s.getId(), s.getPayerType(), s.getPayerId(),
                s.getPlan().getCode(), s.getPlan().getName(), s.getPlan().getTier(),
                pricingService.parseFeatures(s.getPlan().getFeatures()),
                s.getCurrency(), s.getAmountCents(), s.getProvider(), s.getStatus(),
                s.getCurrentPeriodStart(), s.getCurrentPeriodEnd(), s.isCancelAtPeriodEnd(),
                usage);
    }

    /** Incrementa contador de uso de uma feature no periodo atual. */
    @Transactional
    public void incrementUsage(Long subscriptionId, String featureKey) {
        String yyyymm = YearMonth.now().toString();
        SubscriptionUsage u = usageRepository
                .findBySubscriptionIdAndFeatureKeyAndPeriodYyyymm(subscriptionId, featureKey, yyyymm)
                .orElseGet(() -> {
                    Subscription s = subscriptionRepository.getReferenceById(subscriptionId);
                    return SubscriptionUsage.builder()
                            .subscription(s)
                            .featureKey(featureKey)
                            .periodYyyymm(yyyymm)
                            .count(0)
                            .updatedAt(LocalDateTime.now())
                            .build();
                });
        u.setCount(u.getCount() + 1);
        u.setUpdatedAt(LocalDateTime.now());
        usageRepository.save(u);
    }

    @Transactional(readOnly = true)
    public int getCurrentMonthUsage(Long subscriptionId, String featureKey) {
        String yyyymm = YearMonth.now().toString();
        return usageRepository
                .findBySubscriptionIdAndFeatureKeyAndPeriodYyyymm(subscriptionId, featureKey, yyyymm)
                .map(SubscriptionUsage::getCount)
                .orElse(0);
    }
}
