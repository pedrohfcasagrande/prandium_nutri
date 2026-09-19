package com.nutritracker.service;

import com.nutritracker.domain.PlanTarget;
import com.nutritracker.domain.Subscription;
import com.nutritracker.repository.CompanyNutritionistRepository;
import com.nutritracker.repository.NutritionistClientLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Enforcement de limites por plano. Retorna limites/uso, chama {@link PlanLimitExceededException}
 * quando o limite e atingido.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureGateService {

    private final SubscriptionService subscriptionService;
    private final NutritionistClientLinkRepository nutriClientRepo;
    private final CompanyNutritionistRepository companyNutriRepo;

    // ---------- Nutricionista: convidar cliente ----------
    public void assertCanAddClient(Long nutriId) {
        Subscription sub = subscriptionService.ensureSubscription(PlanTarget.NUTRITIONIST, nutriId, "BRL");
        Map<String, Object> features = getFeatures(sub);
        Integer limit = intFeature(features, "maxClients");
        if (limit == null) return; // ilimitado
        long current = nutriClientRepo.countActiveOrPending(nutriId);
        if (current >= limit) {
            throw new PlanLimitExceededException("maxClients", limit, (int) current, sub.getPlan().getCode());
        }
    }

    // ---------- Company: convidar nutricionista ----------
    public void assertCanAddNutritionist(Long companyId) {
        Optional<Subscription> subOpt = subscriptionService.effectiveOf(PlanTarget.COMPANY, companyId);
        if (subOpt.isEmpty()) return; // sem subscription = admin fez cadastro manual, deixa passar
        Subscription sub = subOpt.get();
        Map<String, Object> features = getFeatures(sub);
        Integer limit = intFeature(features, "maxNutritionists");
        if (limit == null) return;
        long current = companyNutriRepo.findByCompanyIdOrderByInvitedAtDesc(companyId).stream()
                .filter(cn -> cn.getStatus() == com.nutritracker.domain.LinkStatus.ACTIVE
                           || cn.getStatus() == com.nutritracker.domain.LinkStatus.PENDING)
                .count();
        if (current >= limit) {
            throw new PlanLimitExceededException("maxNutritionists", limit, (int) current, sub.getPlan().getCode());
        }
    }

    // ---------- Nutricionista / Cliente: gerar sugestao IA ----------
    public void assertCanUseAiSuggestion(PlanTarget payerType, Long payerId) {
        Subscription sub = subscriptionService.ensureSubscription(payerType, payerId, "BRL");
        Map<String, Object> features = getFeatures(sub);
        Integer limit = intFeature(features, "aiSuggestionsMonthly");
        if (limit == null) return; // ilimitado
        int used = subscriptionService.getCurrentMonthUsage(sub.getId(), "aiSuggestionsMonthly");
        if (used >= limit) {
            throw new PlanLimitExceededException("aiSuggestionsMonthly", limit, used, sub.getPlan().getCode());
        }
    }

    public void recordAiSuggestionUsage(PlanTarget payerType, Long payerId) {
        Subscription sub = subscriptionService.ensureSubscription(payerType, payerId, "BRL");
        subscriptionService.incrementUsage(sub.getId(), "aiSuggestionsMonthly");
    }

    // ---------- Cliente: encontrar/pedir nutricionista (Fase 4D + paywall) ----------
    public void assertCanFindNutri(Long userId) {
        Subscription sub = subscriptionService.ensureSubscription(PlanTarget.USER, userId, "BRL");
        Map<String, Object> features = getFeatures(sub);
        if (!boolFeature(features, "findNutritionist")) {
            throw new PlanLimitExceededException("findNutritionist", 0, 1, sub.getPlan().getCode());
        }
    }

    public boolean canFindNutri(Long userId) {
        try {
            assertCanFindNutri(userId);
            return true;
        } catch (PlanLimitExceededException e) {
            return false;
        }
    }

    // ---------- helpers ----------
    private Map<String, Object> getFeatures(Subscription sub) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(sub.getPlan().getFeatures(),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Integer intFeature(Map<String, Object> f, String key) {
        Object v = f.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        return null;
    }

    private boolean boolFeature(Map<String, Object> f, String key) {
        Object v = f.get(key);
        return v instanceof Boolean b && b;
    }
}
