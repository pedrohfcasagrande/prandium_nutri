package com.nutritracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutritracker.domain.*;
import com.nutritracker.repository.CompanyRepository;
import com.nutritracker.repository.SubscriptionRepository;
import com.nutritracker.repository.UserRepository;
import com.nutritracker.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * Processa eventos de webhook dos provedores (idempotente via webhook_events).
 * Cria/atualiza Subscription no banco baseado no evento.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookService {

    private final WebhookEventRepository webhookRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final UserRepository userRepo;
    private final CompanyRepository companyRepo;
    private final PricingService pricingService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestClient restClient;

    @Value("${nutritracker.mercadopago.access-token:}")
    private String mpAccessToken;

    // ============================== STRIPE ==============================

    @Transactional
    public void processStripeEvent(String eventId, String type, String rawPayload) {
        if (isDuplicate(PaymentProvider.STRIPE, eventId)) return;
        try {
            JsonNode root = mapper.readTree(rawPayload);
            JsonNode obj = root.path("data").path("object");

            switch (type) {
                case "checkout.session.completed" -> handleCheckoutCompleted(obj);
                case "customer.subscription.updated",
                     "customer.subscription.created" -> upsertFromStripeSubscription(obj);
                case "customer.subscription.deleted" -> markCanceled(obj);
                case "invoice.payment_failed" -> markPastDue(obj);
                default -> log.info("Evento Stripe ignorado: {}", type);
            }
            recordWebhook(PaymentProvider.STRIPE, eventId, type, rawPayload);
        } catch (Exception e) {
            log.error("Erro processando webhook Stripe {}", eventId, e);
            throw new RuntimeException(e);
        }
    }

    private void handleCheckoutCompleted(JsonNode session) {
        String subscriptionId = session.path("subscription").asText(null);
        String clientReferenceId = session.path("client_reference_id").asText(null);
        String planCode = session.path("metadata").path("planCode").asText(null);
        if (subscriptionId == null || clientReferenceId == null || planCode == null) return;

        // Cria subscription ACTIVE (dados basicos; detalhes vao ser refinados pelo subscription.updated)
        Long userId = Long.parseLong(clientReferenceId);
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return;

        Plan plan = pricingService.getPlanOrThrow(planCode);
        PlanTarget target = plan.getTarget();
        Long payerId = resolvePayerId(user, target);
        if (payerId == null) return;

        cancelExistingSubscriptions(target, payerId);

        String currency = session.path("currency").asText("BRL").toUpperCase();
        int amountTotal = session.path("amount_total").asInt(0);

        Subscription sub = Subscription.builder()
                .payerType(target)
                .payerId(payerId)
                .plan(plan)
                .currency(currency)
                .amountCents(amountTotal)
                .provider(PaymentProvider.STRIPE)
                .providerSubscriptionId(subscriptionId)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
                .build();
        subscriptionRepo.save(sub);
        log.info("Subscription Stripe criada: {} (user {})", subscriptionId, userId);
    }

    private void upsertFromStripeSubscription(JsonNode obj) {
        String stripeSubId = obj.path("id").asText(null);
        if (stripeSubId == null) return;

        Subscription sub = findByProviderId(stripeSubId);
        if (sub == null) return;

        String stripeStatus = obj.path("status").asText("");
        sub.setStatus(mapStripeStatus(stripeStatus));
        sub.setCancelAtPeriodEnd(obj.path("cancel_at_period_end").asBoolean(false));

        long periodEnd = obj.path("current_period_end").asLong(0);
        if (periodEnd > 0) {
            sub.setCurrentPeriodEnd(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(periodEnd), ZoneId.systemDefault()));
        }
        long periodStart = obj.path("current_period_start").asLong(0);
        if (periodStart > 0) {
            sub.setCurrentPeriodStart(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(periodStart), ZoneId.systemDefault()));
        }
        subscriptionRepo.save(sub);
    }

    private void markCanceled(JsonNode obj) {
        String id = obj.path("id").asText(null);
        Subscription sub = findByProviderId(id);
        if (sub == null) return;
        sub.setStatus(SubscriptionStatus.CANCELED);
        sub.setCanceledAt(LocalDateTime.now());
        subscriptionRepo.save(sub);
    }

    private void markPastDue(JsonNode obj) {
        String subId = obj.path("subscription").asText(null);
        if (subId == null) return;
        Subscription sub = findByProviderId(subId);
        if (sub == null) return;
        sub.setStatus(SubscriptionStatus.PAST_DUE);
        subscriptionRepo.save(sub);
    }

    private SubscriptionStatus mapStripeStatus(String s) {
        return switch (s) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "trialing" -> SubscriptionStatus.TRIALING;
            case "past_due", "unpaid" -> SubscriptionStatus.PAST_DUE;
            case "canceled" -> SubscriptionStatus.CANCELED;
            default -> SubscriptionStatus.ACTIVE;
        };
    }

    // ============================== MERCADO PAGO ==============================

    @Transactional
    public void processMercadoPagoEvent(String eventId, String rawPayload) {
        if (isDuplicate(PaymentProvider.MERCADO_PAGO, eventId)) return;
        try {
            JsonNode root = mapper.readTree(rawPayload);
            String type = root.path("type").asText(root.path("action").asText(""));
            String resourceId = root.path("data").path("id").asText(null);

            if ("preapproval".equals(type) || "subscription_preapproval".equals(type)) {
                fetchAndUpsertMpPreapproval(resourceId);
            } else if ("preapproval_authorized_payment".equals(type)) {
                // Pagamento autorizado; opcionalmente forcar sync do preapproval
                fetchAndUpsertMpPreapproval(root.path("data").path("preapproval_id").asText(null));
            } else {
                log.info("Evento MP ignorado: {}", type);
            }
            recordWebhook(PaymentProvider.MERCADO_PAGO, eventId, type, rawPayload);
        } catch (Exception e) {
            log.error("Erro processando webhook MP {}", eventId, e);
            throw new RuntimeException(e);
        }
    }

    private void fetchAndUpsertMpPreapproval(String preapprovalId) {
        if (preapprovalId == null || preapprovalId.isBlank() || mpAccessToken == null || mpAccessToken.isBlank()) return;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> preapp = restClient.get()
                    .uri("https://api.mercadopago.com/preapproval/{id}", preapprovalId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + mpAccessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);
            if (preapp == null) return;

            String status = (String) preapp.get("status");
            String externalRef = (String) preapp.get("external_reference");
            if (externalRef == null) return;

            // external_reference = "user_{id}_plan_{code}"
            String[] parts = externalRef.split("_", 4);
            if (parts.length < 4 || !"user".equals(parts[0]) || !"plan".equals(parts[2])) return;
            Long userId = Long.parseLong(parts[1]);
            String planCode = parts[3];
            User user = userRepo.findById(userId).orElse(null);
            if (user == null) return;
            Plan plan = pricingService.getPlanOrThrow(planCode);
            PlanTarget target = plan.getTarget();
            Long payerId = resolvePayerId(user, target);
            if (payerId == null) return;

            Subscription sub = findByProviderId(preapprovalId);
            if (sub == null) {
                // Cria caso ainda nao exista
                cancelExistingSubscriptions(target, payerId);
                @SuppressWarnings("unchecked")
                Map<String, Object> autoRec = (Map<String, Object>) preapp.get("auto_recurring");
                String currency = autoRec != null ? (String) autoRec.get("currency_id") : "BRL";
                double amount = autoRec != null ? ((Number) autoRec.get("transaction_amount")).doubleValue() : 0;
                sub = Subscription.builder()
                        .payerType(target)
                        .payerId(payerId)
                        .plan(plan)
                        .currency(currency)
                        .amountCents((int) Math.round(amount * 100))
                        .provider(PaymentProvider.MERCADO_PAGO)
                        .providerSubscriptionId(preapprovalId)
                        .status(mapMpStatus(status))
                        .currentPeriodStart(LocalDateTime.now())
                        .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
                        .build();
            } else {
                sub.setStatus(mapMpStatus(status));
            }
            subscriptionRepo.save(sub);
        } catch (Exception e) {
            log.error("Falha ao buscar preapproval MP {}: {}", preapprovalId, e.getMessage());
        }
    }

    private SubscriptionStatus mapMpStatus(String s) {
        if (s == null) return SubscriptionStatus.ACTIVE;
        return switch (s.toLowerCase()) {
            case "authorized" -> SubscriptionStatus.ACTIVE;
            case "pending" -> SubscriptionStatus.TRIALING;
            case "paused" -> SubscriptionStatus.PAST_DUE;
            case "cancelled" -> SubscriptionStatus.CANCELED;
            default -> SubscriptionStatus.ACTIVE;
        };
    }

    // ============================== helpers ==============================

    private boolean isDuplicate(PaymentProvider provider, String eventId) {
        if (eventId == null || eventId.isBlank()) return false;
        return webhookRepo.existsByProviderAndProviderEventId(provider, eventId);
    }

    private void recordWebhook(PaymentProvider provider, String eventId, String type, String payload) {
        if (eventId == null || eventId.isBlank()) return;
        webhookRepo.save(WebhookEvent.builder()
                .provider(provider)
                .providerEventId(eventId)
                .eventType(type)
                .payload(payload)
                .build());
    }

    private Subscription findByProviderId(String providerSubId) {
        if (providerSubId == null) return null;
        return subscriptionRepo.findAll().stream()
                .filter(s -> providerSubId.equals(s.getProviderSubscriptionId()))
                .findFirst()
                .orElse(null);
    }

    private Long resolvePayerId(User user, PlanTarget target) {
        return switch (target) {
            case COMPANY -> companyRepo.findByOwnerId(user.getId())
                    .map(c -> c.getId())
                    .orElse(null);
            case NUTRITIONIST, USER -> user.getId();
        };
    }

    private void cancelExistingSubscriptions(PlanTarget target, Long payerId) {
        for (Subscription s : subscriptionRepo.findByPayerTypeAndPayerIdOrderByStartedAtDesc(target, payerId)) {
            if (s.getStatus() == SubscriptionStatus.ACTIVE
                    || s.getStatus() == SubscriptionStatus.TRIALING) {
                s.setStatus(SubscriptionStatus.CANCELED);
                s.setCanceledAt(LocalDateTime.now());
                subscriptionRepo.save(s);
            }
        }
    }
}
