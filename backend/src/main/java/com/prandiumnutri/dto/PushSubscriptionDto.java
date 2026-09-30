package com.prandiumnutri.dto;

/**
 * Payload que o browser gera via {@code pushManager.subscribe()}.
 * {@code endpoint} e a URL do push service; {@code keys.p256dh}/{@code keys.auth}
 * viram os campos aqui.
 */
public record PushSubscriptionDto(
        String endpoint,
        String p256dh,
        String auth,
        String userAgent
) {}
