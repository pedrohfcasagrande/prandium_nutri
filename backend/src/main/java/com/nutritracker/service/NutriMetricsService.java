package com.nutritracker.service;

import com.nutritracker.domain.AuditAction;
import com.nutritracker.domain.LinkStatus;
import com.nutritracker.domain.NutritionistClientLink;
import com.nutritracker.domain.PlanTarget;
import com.nutritracker.dto.NutriMetricsDto.ClientRow;
import com.nutritracker.dto.NutriMetricsDto.Overview;
import com.nutritracker.repository.AuditLogRepository;
import com.nutritracker.repository.MealRepository;
import com.nutritracker.repository.MenuPlanRepository;
import com.nutritracker.repository.NutritionistClientLinkRepository;
import com.nutritracker.repository.SubscriptionRepository;
import com.nutritracker.repository.SubscriptionUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NutriMetricsService {

    private final NutritionistClientLinkRepository linkRepository;
    private final MealRepository mealRepository;
    private final MenuPlanRepository menuPlanRepository;
    private final AuditLogRepository auditRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionUsageRepository usageRepository;
    private final PricingService pricingService;

    public Overview overview(Long nutriId) {
        List<NutritionistClientLink> links = linkRepository.findByNutritionistIdOrderByInvitedAtDesc(nutriId).stream()
                .filter(l -> l.getStatus() == LinkStatus.ACTIVE)
                .toList();

        LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();
        long aiUsage = auditRepository.countByActionAndActorSince(
                AuditAction.SUGGESTION_GENERATED, nutriId, monthStart);

        long aiLimit = -1;
        var sub = subscriptionRepository.findFirstByPayerTypeAndPayerIdAndStatusInOrderByStartedAtDesc(
                PlanTarget.NUTRITIONIST, nutriId,
                List.of(com.nutritracker.domain.SubscriptionStatus.ACTIVE,
                        com.nutritracker.domain.SubscriptionStatus.TRIALING));
        if (sub.isPresent()) {
            var features = pricingService.parseFeatures(sub.get().getPlan().getFeatures());
            Object raw = features.get("aiSuggestionsMonthly");
            aiLimit = raw instanceof Number n ? n.longValue() : -1;
        }

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        List<ClientRow> rows = new ArrayList<>();
        long menusActive = 0;
        double sumAdherence = 0;
        long needsAttention = 0;

        for (NutritionistClientLink l : links) {
            var client = l.getClient();
            if (client == null) continue;

            long distinctDays = mealRepository.countDistinctDatesSince(client.getId(), weekAgo);
            double adherence = distinctDays / 7.0;
            sumAdherence += adherence;

            boolean attn = adherence < 0.30;
            if (attn) needsAttention++;

            LocalDate lastMeal = mealRepository.lastMealDate(client.getId());
            boolean hasPlan = menuPlanRepository.findFirstByUserIdAndActiveTrueOrderByCreatedAtDesc(client.getId()).isPresent();
            if (hasPlan) menusActive++;

            rows.add(new ClientRow(
                    client.getId(),
                    client.getName(),
                    client.getEmail(),
                    Math.round(adherence * 100) / 100.0,
                    lastMeal != null ? lastMeal.toString() : null,
                    attn,
                    hasPlan
            ));
        }

        double avgAdherence = links.isEmpty() ? 0 : sumAdherence / links.size();

        return new Overview(
                links.size(), aiUsage, aiLimit, menusActive,
                Math.round(avgAdherence * 100) / 100.0,
                needsAttention, rows);
    }
}
