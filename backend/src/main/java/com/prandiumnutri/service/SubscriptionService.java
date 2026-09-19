package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.dto.SubscriptionDto;

import com.prandiumnutri.repository.CompanyNutritionistRepository;
import com.prandiumnutri.repository.NutritionistClientLinkRepository;
import com.prandiumnutri.repository.SubscriptionRepository;
import com.prandiumnutri.repository.SubscriptionUsageRepository;
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
    private final NutritionistClientLinkRepository nutriClientRepo;
    private final CompanyNutritionistRepository companyNutriRepo;
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

    /**
     * Fase 5F: subscription "efetiva" do contexto do nutri.
     * Se a practice esta vinculada a uma empresa, os limites vem da subscription da empresa;
     * caso contrario (particular), vem da subscription pessoal do nutri.
     */
    @Transactional(readOnly = true)
    public Optional<Subscription> effectiveForPractice(com.prandiumnutri.domain.NutritionistPractice practice) {
        if (practice == null) return Optional.empty();
        if (practice.getCompany() != null) {
            return effectiveOf(PlanTarget.COMPANY, practice.getCompany().getId());
        }
        return effectiveOf(PlanTarget.NUTRITIONIST, practice.getNutritionist().getId());
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
        // Counters mensais (incrementados via incrementUsage)
        for (String key : List.of("aiSuggestionsMonthly")) {
            usageRepository.findBySubscriptionIdAndFeatureKeyAndPeriodYyyymm(s.getId(), key, yyyymm)
                    .ifPresent(u -> usage.put(key, u.getCount()));
        }
        // Counts real-time (headcount) — refletem estado do banco, nao counter
        Map<String, Object> features = pricingService.parseFeatures(s.getPlan().getFeatures());
        if (s.getPayerType() == PlanTarget.NUTRITIONIST && features.containsKey("maxClients")) {
            usage.put("maxClients", (int) nutriClientRepo.countActiveOrPending(s.getPayerId()));
        }
        if (s.getPayerType() == PlanTarget.COMPANY && features.containsKey("maxNutritionists")) {
            long count = companyNutriRepo.findByCompanyIdOrderByInvitedAtDesc(s.getPayerId()).stream()
                    .filter(cn -> cn.getStatus() == com.prandiumnutri.domain.LinkStatus.ACTIVE
                               || cn.getStatus() == com.prandiumnutri.domain.LinkStatus.PENDING)
                    .count();
            usage.put("maxNutritionists", (int) count);
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

    // ---------------- Fase 5C: sponsorship ----------------

    /**
     * Ativa PLUS "cortesia" para o cliente porque ele foi vinculado a uma practice.
     * Idempotente. Se ja ha uma subscription paga propria (nao sponsored), nao mexe.
     */
    @Transactional
    public void sponsorClientToPlus(Long clientId, com.prandiumnutri.domain.NutritionistPractice practice) {
        Subscription current = ensureSubscription(PlanTarget.USER, clientId, "BRL");
        // Ja e paga (nao sponsored) — respeita: cliente continua no seu plano proprio.
        if (current.getSponsoredByPractice() == null
                && !"CLIENT_FREE".equals(current.getPlan().getCode())) {
            return;
        }
        // Fase 5G: sponsor so concede PLUS se o pagador (nutri particular ou empresa) tem plano PAGO.
        // Nutri no NUTRI_FREE particular nao pode presentear PLUS.
        if (!payerHasPaidTier(practice)) {
            return;
        }
        Plan plus = pricingService.getPlanOrThrow("CLIENT_PLUS");
        current.setPlan(plus);
        current.setSponsoredByPractice(practice);
        current.setStatus(SubscriptionStatus.ACTIVE);
        current.setProvider(PaymentProvider.MANUAL);
        current.setAmountCents(0);
        subscriptionRepository.save(current);
    }

    /** Fase 5G: verifica se o pagador daquela practice (empresa ou nutri) tem tier != FREE. */
    private boolean payerHasPaidTier(com.prandiumnutri.domain.NutritionistPractice practice) {
        if (practice == null) return false;
        var sub = effectiveForPractice(practice);
        if (sub.isEmpty()) return false;
        String tier = sub.get().getPlan().getTier();
        return tier != null && !"FREE".equalsIgnoreCase(tier);
    }

    /**
     * Cliente desvinculado desta practice. Se a subscription atual e sponsored por ela,
     * transfere o sponsorship para outra practice ativa do cliente (se houver) ou
     * volta a FREE. Nao mexe em subscription paga.
     */
    @Transactional
    public void revokeSponsorship(Long clientId, Long practiceId,
                                   java.util.function.Supplier<java.util.Optional<com.prandiumnutri.domain.NutritionistPractice>> nextSponsorFinder) {
        var subOpt = effectiveOf(PlanTarget.USER, clientId);
        if (subOpt.isEmpty()) return;
        Subscription sub = subOpt.get();
        if (sub.getSponsoredByPractice() == null
                || !sub.getSponsoredByPractice().getId().equals(practiceId)) {
            return; // nao era essa practice que patrocinava
        }
        var next = nextSponsorFinder.get();
        if (next.isPresent()) {
            sub.setSponsoredByPractice(next.get());
            subscriptionRepository.save(sub);
            return;
        }
        // Sem outro sponsor -> volta a FREE
        Plan free = pricingService.getPlanOrThrow("CLIENT_FREE");
        sub.setPlan(free);
        sub.setSponsoredByPractice(null);
        sub.setAmountCents(0);
        subscriptionRepository.save(sub);
    }
}
