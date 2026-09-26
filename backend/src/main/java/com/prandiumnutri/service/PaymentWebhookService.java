package com.prandiumnutri.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prandiumnutri.domain.*;
import com.prandiumnutri.repository.CompanyRepository;
import com.prandiumnutri.repository.SubscriptionRepository;
import com.prandiumnutri.repository.UserRepository;
import com.prandiumnutri.repository.WebhookEventRepository;
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

    @Value("${prandiumnutri.mercadopago.access-token:}")
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
                case "invoice.payment_succeeded" -> markInvoicePaid(obj);
                default -> log.info("Evento Stripe ignorado: {}", type);
            }
            recordWebhook(PaymentProvider.STRIPE, eventId, type, rawPayload);
        } catch (Exception e) {
            log.error("Erro processando webhook Stripe {}", eventId, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * checkout.session.completed: tenta criar a subscription (se ainda nao existir).
     * Como o subscription.created pode chegar antes ou depois, ambos os handlers
     * fazem upsert idempotente pelo providerSubscriptionId.
     */
    private void handleCheckoutCompleted(JsonNode session) {
        String subscriptionId = session.path("subscription").asText(null);
        String planCode = session.path("metadata").path("planCode").asText(null);
        String clientReferenceId = session.path("client_reference_id").asText(null);
        if (subscriptionId == null || planCode == null || clientReferenceId == null) return;
        if (subscriptionRepo.findByProviderSubscriptionId(subscriptionId).isPresent()) return;

        Long userId = parseLong(clientReferenceId);
        if (userId == null) return;

        String currency = session.path("currency").asText("BRL").toUpperCase();
        int amount = session.path("amount_total").asInt(0);

        createStripeSubscription(subscriptionId, userId, planCode, currency, amount,
                LocalDateTime.now(), LocalDateTime.now().plusMonths(1),
                SubscriptionStatus.ACTIVE, false);
    }

    /**
     * customer.subscription.created / updated: upsert baseado no proprio payload da subscription.
     * Se ainda nao existe em banco, cria (cobre o caso em que este evento chega antes do
     * checkout.session.completed, sem ordem garantida).
     */
    private void upsertFromStripeSubscription(JsonNode obj) {
        String stripeSubId = obj.path("id").asText(null);
        if (stripeSubId == null) return;

        SubscriptionStatus status = mapStripeStatus(obj.path("status").asText(""));
        boolean cancelAtPeriodEnd = obj.path("cancel_at_period_end").asBoolean(false);
        LocalDateTime periodStart = epochToLocal(obj.path("current_period_start").asLong(0));
        LocalDateTime periodEnd = epochToLocal(obj.path("current_period_end").asLong(0));

        var existing = subscriptionRepo.findByProviderSubscriptionId(stripeSubId);
        if (existing.isPresent()) {
            Subscription sub = existing.get();
            sub.setStatus(status);
            sub.setCancelAtPeriodEnd(cancelAtPeriodEnd);
            if (periodStart != null) sub.setCurrentPeriodStart(periodStart);
            if (periodEnd != null) sub.setCurrentPeriodEnd(periodEnd);
            subscriptionRepo.save(sub);
            return;
        }

        // Ainda nao existe: reconstroi a partir do metadata da propria subscription.
        String planCode = obj.path("metadata").path("planCode").asText(null);
        Long userId = parseLong(obj.path("metadata").path("payerUserId").asText(null));
        if (planCode == null || userId == null) {
            log.warn("Stripe subscription {} sem metadata planCode/payerUserId; nao foi possivel criar", stripeSubId);
            return;
        }

        JsonNode firstItemPrice = obj.path("items").path("data").path(0).path("price");
        String currency = firstItemPrice.path("currency").asText("brl").toUpperCase();
        int amount = firstItemPrice.path("unit_amount").asInt(0);

        LocalDateTime effectiveStart = periodStart != null ? periodStart : LocalDateTime.now();
        LocalDateTime effectiveEnd = periodEnd != null ? periodEnd : LocalDateTime.now().plusMonths(1);

        createStripeSubscription(stripeSubId, userId, planCode, currency, amount,
                effectiveStart, effectiveEnd, status, cancelAtPeriodEnd);
    }

    private void createStripeSubscription(String providerSubId, Long userId, String planCode,
                                          String currency, int amountCents,
                                          LocalDateTime periodStart, LocalDateTime periodEnd,
                                          SubscriptionStatus status, boolean cancelAtPeriodEnd) {
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Stripe: usuario {} nao encontrado ao criar subscription {}", userId, providerSubId);
            return;
        }
        Plan plan = pricingService.getPlanOrThrow(planCode);
        Long payerId = resolvePayerId(user, plan.getTarget());
        if (payerId == null) return;

        cancelExistingSubscriptions(plan.getTarget(), payerId);

        Subscription sub = Subscription.builder()
                .payerType(plan.getTarget())
                .payerId(payerId)
                .plan(plan)
                .currency(currency)
                .amountCents(amountCents)
                .provider(PaymentProvider.STRIPE)
                .providerSubscriptionId(providerSubId)
                .status(status)
                .cancelAtPeriodEnd(cancelAtPeriodEnd)
                .currentPeriodStart(periodStart)
                .currentPeriodEnd(periodEnd)
                .build();
        subscriptionRepo.save(sub);
        log.info("Subscription Stripe criada: {} (user {}, plan {})", providerSubId, userId, planCode);
    }

    private void markCanceled(JsonNode obj) {
        String id = obj.path("id").asText(null);
        subscriptionRepo.findByProviderSubscriptionId(id).ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.CANCELED);
            sub.setCanceledAt(LocalDateTime.now());
            subscriptionRepo.save(sub);
        });
    }

    private void markPastDue(JsonNode invoiceObj) {
        String subId = invoiceObj.path("subscription").asText(null);
        if (subId == null) return;
        subscriptionRepo.findByProviderSubscriptionId(subId).ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.PAST_DUE);
            subscriptionRepo.save(sub);
        });
    }

    /**
     * invoice.payment_succeeded: pagamento (inicial ou renovacao) confirmado.
     * Garante que a sub volte pra ACTIVE (caso estivesse PAST_DUE) e avanca o period_end
     * conforme o period da invoice, se disponivel.
     */
    private void markInvoicePaid(JsonNode invoiceObj) {
        String subId = invoiceObj.path("subscription").asText(null);
        if (subId == null) return;
        subscriptionRepo.findByProviderSubscriptionId(subId).ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.ACTIVE);
            LocalDateTime periodEnd = epochToLocal(invoiceObj.path("period_end").asLong(0));
            LocalDateTime periodStart = epochToLocal(invoiceObj.path("period_start").asLong(0));
            if (periodEnd != null) sub.setCurrentPeriodEnd(periodEnd);
            if (periodStart != null) sub.setCurrentPeriodStart(periodStart);
            subscriptionRepo.save(sub);
        });
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

            Subscription sub = subscriptionRepo.findByProviderSubscriptionId(preapprovalId).orElse(null);
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

    private static LocalDateTime epochToLocal(long epochSeconds) {
        if (epochSeconds <= 0) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
    }

    private static Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
    }
}
