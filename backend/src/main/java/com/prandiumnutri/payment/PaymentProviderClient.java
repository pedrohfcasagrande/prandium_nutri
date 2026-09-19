package com.prandiumnutri.payment;

import com.prandiumnutri.domain.PaymentProvider;
import com.prandiumnutri.domain.Plan;
import com.prandiumnutri.domain.PlanPrice;
import com.prandiumnutri.domain.Subscription;
import com.prandiumnutri.domain.User;

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
