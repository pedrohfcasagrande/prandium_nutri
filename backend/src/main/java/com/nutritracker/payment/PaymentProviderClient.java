package com.nutritracker.payment;

import com.nutritracker.domain.PaymentProvider;
import com.nutritracker.domain.Plan;
import com.nutritracker.domain.PlanPrice;
import com.nutritracker.domain.Subscription;
import com.nutritracker.domain.User;

/** Contrato para provedores de pagamento (Stripe, Mercado Pago). */
public interface PaymentProviderClient {

    PaymentProvider provider();

    boolean isConfigured();

    /** Cria uma sessao de checkout (retorna URL onde o cliente vai completar o pagamento). */
    CheckoutSessionResult createCheckout(User payer, Plan plan, PlanPrice price,
                                          String successUrl, String cancelUrl);

    /** Cancela subscription no provider. atPeriodEnd=true faz cancelamento agendado. */
    void cancelSubscription(Subscription sub, boolean atPeriodEnd);
}
