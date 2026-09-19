package com.prandiumnutri.service;

import com.prandiumnutri.domain.PlanTarget;
import com.prandiumnutri.domain.Subscription;
import com.prandiumnutri.repository.CompanyNutritionistRepository;
import com.prandiumnutri.repository.NutritionistClientLinkRepository;
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
    private final NutritionistPracticeService practiceService;

    // ---------- Nutricionista: convidar cliente ----------
    public void assertCanAddClient(Long nutriId) {
        assertCanAddClientForPractice(nutriId, null);
    }

    /**
     * Fase 5F: se {@code practiceId} pertence a uma practice vinculada a empresa,
     * o limite usado e o {@code maxClientsPerNutri} do plano da empresa (contando so
     * clientes daquela practice). Caso contrario (practice particular ou null),
     * usa {@code maxClients} do plano pessoal do nutri (contando clientes de qualquer practice).
     */
    public void assertCanAddClientForPractice(Long nutriId, Long practiceId) {
        var practice = practiceId == null ? null : practiceService.getOrThrow(practiceId);
        if (practice != null && practice.getCompany() != null) {
            var subOpt = subscriptionService.effectiveOf(PlanTarget.COMPANY, practice.getCompany().getId());
            if (subOpt.isEmpty()) return;
            Subscription sub = subOpt.get();
            Map<String, Object> features = getFeatures(sub);
            Integer limit = intFeature(features, "maxClientsPerNutri");
            if (limit == null) return;
            long current = nutriClientRepo.findByNutritionistIdOrderByInvitedAtDesc(nutriId).stream()
                    .filter(l -> l.getPractice() != null && l.getPractice().getId().equals(practiceId))
                    .filter(l -> l.getStatus() == com.prandiumnutri.domain.LinkStatus.ACTIVE
                              || l.getStatus() == com.prandiumnutri.domain.LinkStatus.PENDING)
                    .count();
            if (current >= limit) {
                throw new PlanLimitExceededException("maxClientsPerNutri", limit, (int) current, sub.getPlan().getCode());
            }
            return;
        }
        // Contexto particular
        Subscription sub = subscriptionService.ensureSubscription(PlanTarget.NUTRITIONIST, nutriId, "BRL");
        Map<String, Object> features = getFeatures(sub);
        Integer limit = intFeature(features, "maxClients");
        if (limit == null) return;
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
                .filter(cn -> cn.getStatus() == com.prandiumnutri.domain.LinkStatus.ACTIVE
                           || cn.getStatus() == com.prandiumnutri.domain.LinkStatus.PENDING)
                .count();
        if (current >= limit) {
            throw new PlanLimitExceededException("maxNutritionists", limit, (int) current, sub.getPlan().getCode());
        }
    }

    // ---------- Nutricionista / Cliente: gerar sugestao IA ----------
    public void assertCanUseAiSuggestion(PlanTarget payerType, Long payerId) {
        assertCanUseAiSuggestion(payerType, payerId, null);
    }

    /**
     * Fase 5F: se o payer e NUTRITIONIST e practiceId aponta pra practice vinculada,
     * o gate consulta a subscription da EMPRESA (limite compartilhado) em vez da
     * subscription pessoal do nutri.
     */
    public void assertCanUseAiSuggestion(PlanTarget payerType, Long payerId, Long practiceId) {
        Subscription sub = resolveSubscription(payerType, payerId, practiceId);
        Map<String, Object> features = getFeatures(sub);
        Integer limit = intFeature(features, "aiSuggestionsMonthly");
        if (limit == null) return; // ilimitado
        int used = subscriptionService.getCurrentMonthUsage(sub.getId(), "aiSuggestionsMonthly");
        if (used >= limit) {
            throw new PlanLimitExceededException("aiSuggestionsMonthly", limit, used, sub.getPlan().getCode());
        }
    }

    public void recordAiSuggestionUsage(PlanTarget payerType, Long payerId) {
        recordAiSuggestionUsage(payerType, payerId, null);
    }

    public void recordAiSuggestionUsage(PlanTarget payerType, Long payerId, Long practiceId) {
        Subscription sub = resolveSubscription(payerType, payerId, practiceId);
        subscriptionService.incrementUsage(sub.getId(), "aiSuggestionsMonthly");
    }

    /** Escolhe a subscription cujos features/counters serao usados no gate. */
    private Subscription resolveSubscription(PlanTarget payerType, Long payerId, Long practiceId) {
        if (payerType == PlanTarget.NUTRITIONIST && practiceId != null) {
            var practice = practiceService.getOrThrow(practiceId);
            if (practice.getCompany() != null) {
                var companySub = subscriptionService.effectiveOf(
                        PlanTarget.COMPANY, practice.getCompany().getId());
                if (companySub.isPresent()) return companySub.get();
            }
        }
        return subscriptionService.ensureSubscription(payerType, payerId, "BRL");
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
