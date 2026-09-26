package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.repository.PlanPriceRepository;
import com.prandiumnutri.repository.PlanRepository;
import com.prandiumnutri.repository.SubscriptionRepository;
import com.prandiumnutri.repository.UserRepository;
import com.prandiumnutri.repository.WebhookEventRepository;
import com.prandiumnutri.support.PlanTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentWebhookServiceTest {

    @Autowired PaymentWebhookService service;
    @Autowired SubscriptionRepository subRepo;
    @Autowired UserRepository userRepo;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;
    @Autowired WebhookEventRepository webhookRepo;

    private User user;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
        user = userRepo.save(User.builder()
                .name("Nutri Webhook")
                .email("wh-" + System.nanoTime() + "@example.com")
                .passwordHash("$2a$10$xxx")
                .role(Role.NUTRITIONIST)
                .provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true)
                .consentAcceptedAt(LocalDateTime.now())
                .cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
                .build());
    }

    // ============================== dedup ==============================

    @Test
    void dedup_sameEventIdProcessedTwice_recordedOnlyOnce() {
        String eventId = "evt_dedup_" + System.nanoTime();
        String subId = "sub_dedup_" + System.nanoTime();
        String payload = stripeCheckoutCompleted(subId, user.getId(), "NUTRI_PRO", "BRL", 7900);

        service.processStripeEvent(eventId, "checkout.session.completed", payload);
        service.processStripeEvent(eventId, "checkout.session.completed", payload);

        assertThat(webhookRepo.findAll().stream()
                .filter(w -> eventId.equals(w.getProviderEventId()))
                .count()).isEqualTo(1);
        assertThat(subRepo.findByProviderSubscriptionId(subId)).isPresent();
    }

    // ============================== checkout.session.completed ==============================

    @Test
    void checkoutCompleted_createsActiveSubscriptionWithPlanAndAmount() {
        String subId = "sub_ck_" + System.nanoTime();
        service.processStripeEvent("evt_" + System.nanoTime(),
                "checkout.session.completed",
                stripeCheckoutCompleted(subId, user.getId(), "NUTRI_PRO", "USD", 1500));

        Subscription sub = subRepo.findByProviderSubscriptionId(subId).orElseThrow();
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.getPlan().getCode()).isEqualTo("NUTRI_PRO");
        assertThat(sub.getPayerId()).isEqualTo(user.getId());
        assertThat(sub.getCurrency()).isEqualTo("USD");
        assertThat(sub.getAmountCents()).isEqualTo(1500);
        assertThat(sub.getProvider()).isEqualTo(PaymentProvider.STRIPE);
    }

    // ============================== race: subscription.created ANTES do checkout.completed ==============================

    @Test
    void subscriptionCreatedBeforeCheckout_stillCreatesSubscription() {
        String subId = "sub_race_" + System.nanoTime();
        long start = Instant.now().getEpochSecond();
        long end = start + 30L * 86400;

        // Simula subscription.created chegando ANTES do checkout.session.completed
        service.processStripeEvent("evt_a_" + System.nanoTime(),
                "customer.subscription.created",
                stripeSubscription(subId, user.getId(), "NUTRI_PRO", "BRL", 7900, "active", start, end, false));

        Subscription sub = subRepo.findByProviderSubscriptionId(subId).orElseThrow();
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.getPlan().getCode()).isEqualTo("NUTRI_PRO");
        assertThat(sub.getAmountCents()).isEqualTo(7900);

        // Depois chega o checkout.session.completed com o mesmo subId — nao deve duplicar
        service.processStripeEvent("evt_b_" + System.nanoTime(),
                "checkout.session.completed",
                stripeCheckoutCompleted(subId, user.getId(), "NUTRI_PRO", "BRL", 7900));

        assertThat(subRepo.findByProviderSubscriptionId(subId)).isPresent();
        long total = subRepo.findByPayerTypeAndPayerIdOrderByStartedAtDesc(PlanTarget.NUTRITIONIST, user.getId())
                .stream()
                .filter(s -> subId.equals(s.getProviderSubscriptionId()))
                .count();
        assertThat(total).isEqualTo(1);
    }

    // ============================== invoice.payment_succeeded → recuperacao de PAST_DUE ==============================

    @Test
    void invoicePaid_flipsPastDueBackToActiveAndAdvancesPeriod() {
        String subId = "sub_pd_" + System.nanoTime();
        service.processStripeEvent("evt_" + System.nanoTime(),
                "checkout.session.completed",
                stripeCheckoutCompleted(subId, user.getId(), "NUTRI_PRO", "BRL", 7900));

        // Falha no pagamento: PAST_DUE
        service.processStripeEvent("evt_" + System.nanoTime(),
                "invoice.payment_failed",
                stripeInvoice(subId, 0, 0));
        assertThat(subRepo.findByProviderSubscriptionId(subId).orElseThrow().getStatus())
                .isEqualTo(SubscriptionStatus.PAST_DUE);

        // Pagamento entra: deve voltar pra ACTIVE e avancar o period_end
        long newStart = Instant.now().getEpochSecond();
        long newEnd = newStart + 30L * 86400;
        service.processStripeEvent("evt_" + System.nanoTime(),
                "invoice.payment_succeeded",
                stripeInvoice(subId, newStart, newEnd));

        Subscription sub = subRepo.findByProviderSubscriptionId(subId).orElseThrow();
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.getCurrentPeriodEnd()).isNotNull();
    }

    // ============================== subscription.deleted ==============================

    @Test
    void subscriptionDeleted_marksCanceled() {
        String subId = "sub_del_" + System.nanoTime();
        service.processStripeEvent("evt_" + System.nanoTime(),
                "checkout.session.completed",
                stripeCheckoutCompleted(subId, user.getId(), "NUTRI_PRO", "BRL", 7900));

        service.processStripeEvent("evt_" + System.nanoTime(),
                "customer.subscription.deleted",
                stripeSubscription(subId, user.getId(), "NUTRI_PRO", "BRL", 7900,
                        "canceled", 0, 0, false));

        Subscription sub = subRepo.findByProviderSubscriptionId(subId).orElseThrow();
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(sub.getCanceledAt()).isNotNull();
    }

    // ============================== payloads ==============================

    private static String stripeCheckoutCompleted(String subId, Long userId, String planCode,
                                                   String currency, int amount) {
        return """
               {
                 "id": "evt_x",
                 "type": "checkout.session.completed",
                 "data": {
                   "object": {
                     "id": "cs_x",
                     "subscription": "%s",
                     "client_reference_id": "%d",
                     "currency": "%s",
                     "amount_total": %d,
                     "metadata": { "planCode": "%s", "payerUserId": "%d" }
                   }
                 }
               }
               """.formatted(subId, userId, currency.toLowerCase(), amount, planCode, userId);
    }

    private static String stripeSubscription(String subId, Long userId, String planCode,
                                              String currency, int amount, String status,
                                              long periodStart, long periodEnd, boolean cancelAtPeriodEnd) {
        return """
               {
                 "id": "evt_x",
                 "type": "customer.subscription.updated",
                 "data": {
                   "object": {
                     "id": "%s",
                     "status": "%s",
                     "cancel_at_period_end": %s,
                     "current_period_start": %d,
                     "current_period_end": %d,
                     "metadata": { "planCode": "%s", "payerUserId": "%d" },
                     "items": {
                       "data": [ { "price": { "currency": "%s", "unit_amount": %d } } ]
                     }
                   }
                 }
               }
               """.formatted(subId, status, cancelAtPeriodEnd, periodStart, periodEnd,
                             planCode, userId, currency.toLowerCase(), amount);
    }

    private static String stripeInvoice(String subId, long periodStart, long periodEnd) {
        return """
               {
                 "id": "evt_x",
                 "type": "invoice.payment_succeeded",
                 "data": {
                   "object": {
                     "id": "in_x",
                     "subscription": "%s",
                     "period_start": %d,
                     "period_end": %d
                   }
                 }
               }
               """.formatted(subId, periodStart, periodEnd);
    }
}
