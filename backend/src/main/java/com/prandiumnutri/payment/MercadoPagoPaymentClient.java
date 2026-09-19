package com.prandiumnutri.payment;

import com.prandiumnutri.domain.PaymentProvider;
import com.prandiumnutri.domain.Plan;
import com.prandiumnutri.domain.PlanPrice;
import com.prandiumnutri.domain.Subscription;
import com.prandiumnutri.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Mercado Pago via API "preapproval" (assinatura recorrente).
 * Docs: https://www.mercadopago.com.br/developers/pt/reference/subscriptions/_preapproval/post
 */
@Component
@Profile({"sandbox", "prod"})
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoPaymentClient implements PaymentProviderClient {

    private static final String BASE = "https://api.mercadopago.com";

    @Value("${prandiumnutri.mercadopago.access-token:}")
    private String accessToken;

    private final RestClient restClient;

    @Override
    public PaymentProvider provider() { return PaymentProvider.MERCADO_PAGO; }

    @Override
    public boolean isConfigured() { return accessToken != null && !accessToken.isBlank(); }

    @Override
    public CheckoutSessionResult createCheckout(User payer, Plan plan, PlanPrice price,
                                                 String successUrl, String cancelUrl) {
        if (!isConfigured()) {
            throw new IllegalStateException("Mercado Pago nao esta configurado no servidor.");
        }

        try {
            String startDate = OffsetDateTime.now().plusMinutes(1)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            Map<String, Object> body = Map.of(
                    "reason", "Nutri Tracker - " + plan.getName(),
                    "external_reference", "user_" + payer.getId() + "_plan_" + plan.getCode(),
                    "payer_email", payer.getEmail(),
                    "back_url", successUrl,
                    "auto_recurring", Map.of(
                            "frequency", 1,
                            "frequency_type", "months",
                            "transaction_amount", price.getAmountCents() / 100.0,
                            "currency_id", price.getCurrency(),
                            "start_date", startDate
                    ),
                    "status", "pending"
            );

            Map<?, ?> response = restClient.post()
                    .uri(BASE + "/preapproval")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) throw new RuntimeException("Resposta vazia do MP");
            String initPoint = (String) response.get("init_point");
            String preapprovalId = String.valueOf(response.get("id"));
            return new CheckoutSessionResult(initPoint, preapprovalId);
        } catch (Exception e) {
            log.error("Falha ao criar preapproval no MP", e);
            throw new RuntimeException("Falha ao criar checkout MP: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelSubscription(Subscription sub, boolean atPeriodEnd) {
        if (!isConfigured()) throw new IllegalStateException("Mercado Pago nao configurado");
        if (sub.getProviderSubscriptionId() == null) return;

        // MP nao tem "cancel at period end" nativo; cancela direto.
        // O parametro atPeriodEnd e ignorado; log de aviso se true.
        if (atPeriodEnd) {
            log.warn("MP nao suporta cancel-at-period-end; cancelando imediatamente sub {}",
                    sub.getProviderSubscriptionId());
        }
        try {
            restClient.put()
                    .uri(BASE + "/preapproval/{id}", sub.getProviderSubscriptionId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("status", "cancelled"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao cancelar assinatura MP: " + e.getMessage(), e);
        }
    }
}
