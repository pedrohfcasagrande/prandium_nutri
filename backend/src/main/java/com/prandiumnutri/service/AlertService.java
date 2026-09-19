package com.prandiumnutri.service;

import com.prandiumnutri.domain.Alert;
import com.prandiumnutri.domain.AlertScope;
import com.prandiumnutri.domain.AlertSeverity;
import com.prandiumnutri.domain.LinkStatus;
import com.prandiumnutri.domain.SubscriptionStatus;
import com.prandiumnutri.repository.AlertRepository;
import com.prandiumnutri.repository.MealRepository;
import com.prandiumnutri.repository.NutritionistClientLinkRepository;
import com.prandiumnutri.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NutritionistClientLinkRepository linkRepository;
    private final MealRepository mealRepository;

    @Transactional(readOnly = true)
    public List<Alert> listActive(AlertScope scope, Long scopeId) {
        return scopeId == null
                ? alertRepository.findByScopeAndDismissedAtIsNullOrderByCreatedAtDesc(scope)
                : alertRepository.findByScopeAndScopeIdAndDismissedAtIsNullOrderByCreatedAtDesc(scope, scopeId);
    }

    @Transactional
    public void dismiss(Long alertId, Long userId) {
        alertRepository.findById(alertId).ifPresent(a -> {
            a.setDismissedAt(LocalDateTime.now());
            a.setDismissedByUserId(userId);
            alertRepository.save(a);
        });
    }

    /** Job diario 03:00 UTC (@Scheduled cron). Ativa se a app estiver ligada. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void evaluateAllRules() {
        log.info("[AlertService] Iniciando avaliacao de regras");
        int created = 0;
        created += evaluatePaymentPastDue();
        created += evaluateClientsInactive7d();
        log.info("[AlertService] {} alertas criados", created);
    }

    /** Cria alerta ADMIN + COMPANY para cada subscription PAST_DUE. */
    int evaluatePaymentPastDue() {
        int count = 0;
        LocalDateTime dedupSince = LocalDateTime.now().minusHours(24);
        for (var sub : subscriptionRepository.findAll()) {
            if (sub.getStatus() != SubscriptionStatus.PAST_DUE) continue;
            if (alertRepository.existsByKindAndScopeAndScopeIdAndCreatedAtAfter(
                    "PAYMENT_PAST_DUE", AlertScope.ADMIN, null, dedupSince)) continue;
            alertRepository.save(Alert.builder()
                    .scope(AlertScope.ADMIN)
                    .scopeId(null)
                    .severity(AlertSeverity.WARN)
                    .kind("PAYMENT_PAST_DUE")
                    .title("Assinatura em atraso")
                    .message("Subscription #" + sub.getId() + " (" + sub.getPayerType() + " #" + sub.getPayerId() + ") esta PAST_DUE")
                    .build());
            count++;
        }
        return count;
    }

    /** Cria alerta NUTRITIONIST para cada cliente sem refeicao ha > 7 dias. */
    int evaluateClientsInactive7d() {
        int count = 0;
        LocalDate weekAgo = LocalDate.now().minusDays(7);
        LocalDateTime dedupSince = LocalDateTime.now().minusHours(24);

        for (var link : linkRepository.findAll()) {
            if (link.getStatus() != LinkStatus.ACTIVE) continue;
            var client = link.getClient();
            var nutri = link.getNutritionist();
            if (client == null || nutri == null) continue;

            LocalDate lastMeal = mealRepository.lastMealDate(client.getId());
            boolean inactive = lastMeal == null || lastMeal.isBefore(weekAgo);
            if (!inactive) continue;

            if (alertRepository.existsByKindAndScopeAndScopeIdAndCreatedAtAfter(
                    "CLIENT_INACTIVE_7D", AlertScope.NUTRITIONIST, nutri.getId(), dedupSince)) continue;

            alertRepository.save(Alert.builder()
                    .scope(AlertScope.NUTRITIONIST)
                    .scopeId(nutri.getId())
                    .severity(AlertSeverity.INFO)
                    .kind("CLIENT_INACTIVE_7D")
                    .title("Cliente inativo ha 7+ dias")
                    .message(client.getName() + " nao registrou refeicoes desde " +
                            (lastMeal != null ? lastMeal : "nunca"))
                    .metadata("{\"clientId\":" + client.getId() + "}")
                    .build());
            count++;
        }
        return count;
    }
}
