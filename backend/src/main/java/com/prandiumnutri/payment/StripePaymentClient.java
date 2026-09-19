package com.prandiumnutri.payment;

import com.prandiumnutri.domain.PaymentProvider;
import com.prandiumnutri.domain.Plan;
import com.prandiumnutri.domain.PlanPrice;
import com.prandiumnutri.domain.Subscription;
import com.prandiumnutri.domain.User;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile({"sandbox", "prod"})
@Slf4j
public class StripePaymentClient implements PaymentProviderClient {

    @Value("${prandiumnutri.stripe.secret-key:}")
    private String secretKey;

    @PostConstruct
    void init() {
        if (secretKey != null && !secretKey.isBlank()) {
            Stripe.apiKey = secretKey;
            log.info("Stripe configurado.");
        } else {
            log.info("Stripe NAO configurado (prandiumnutri.stripe.secret-key vazio).");
        }
    }

    @Override
    public PaymentProvider provider() { return PaymentProvider.STRIPE; }

    @Override
    public boolean isConfigured() { return secretKey != null && !secretKey.isBlank(); }

    @Override
    public CheckoutSessionResult createCheckout(User payer, Plan plan, PlanPrice price,
                                                 String successUrl, String cancelUrl) {
        if (!isConfigured()) {
            throw new IllegalStateException("Stripe nao esta configurado no servidor.");
        }

        try {
            SessionCreateParams.LineItem.PriceData.Recurring recurring =
                    SessionCreateParams.LineItem.PriceData.Recurring.builder()
                            .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                            .build();

            SessionCreateParams.LineItem.PriceData priceData =
                    SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(price.getCurrency().toLowerCase())
                            .setUnitAmount((long) price.getAmountCents())
                            .setRecurring(recurring)
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Nutri Tracker - " + plan.getName())
                                    .build())
                            .build();

            SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                    .setPriceData(priceData)
                    .setQuantity(1L)
                    .build();

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .setCustomerEmail(payer.getEmail())
                    .setClientReferenceId(String.valueOf(payer.getId()))
                    .putMetadata("planCode", plan.getCode())
                    .putMetadata("payerUserId", String.valueOf(payer.getId()))
                    .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                            .putMetadata("planCode", plan.getCode())
                            .putMetadata("payerUserId", String.valueOf(payer.getId()))
                            .build())
                    .addLineItem(lineItem)
                    .build();

            Session session = Session.create(params);
            return new CheckoutSessionResult(session.getUrl(), session.getId());
        } catch (Exception e) {
            log.error("Falha ao criar sessao de checkout Stripe", e);
            throw new RuntimeException("Falha ao criar checkout: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelSubscription(Subscription sub, boolean atPeriodEnd) {
        if (!isConfigured()) throw new IllegalStateException("Stripe nao configurado");
        if (sub.getProviderSubscriptionId() == null) return;
        try {
            com.stripe.model.Subscription stripeSub =
                    com.stripe.model.Subscription.retrieve(sub.getProviderSubscriptionId());
            if (atPeriodEnd) {
                stripeSub.update(SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(true).build());
            } else {
                stripeSub.cancel();
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao cancelar subscription Stripe: " + e.getMessage(), e);
        }
    }
}
