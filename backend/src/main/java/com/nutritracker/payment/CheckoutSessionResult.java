package com.nutritracker.payment;

/** Resultado de criacao de sessao de checkout. */
public record CheckoutSessionResult(
        String checkoutUrl,
        String providerSessionId
) {}
