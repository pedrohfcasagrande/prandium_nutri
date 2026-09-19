package com.prandiumnutri.controller;

import com.prandiumnutri.service.PaymentWebhookService;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/stripe")
@Slf4j
public class StripeWebhookController {

    private final PaymentWebhookService webhookService;
    private final String signingSecret;

    public StripeWebhookController(PaymentWebhookService webhookService,
                                    @Value("${prandiumnutri.stripe.webhook-secret:}") String signingSecret) {
        this.webhookService = webhookService;
        this.signingSecret = signingSecret;
    }

    @PostMapping
    public ResponseEntity<String> receive(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        Event event;
        try {
            if (signingSecret != null && !signingSecret.isBlank() && signature != null) {
                event = Webhook.constructEvent(payload, signature, signingSecret);
            } else {
                log.warn("Stripe webhook sem validacao de assinatura (secret ausente)");
                event = com.stripe.net.ApiResource.GSON.fromJson(payload, Event.class);
            }
        } catch (Exception e) {
            log.warn("Assinatura Stripe invalida: {}", e.getMessage());
            return ResponseEntity.badRequest().body("invalid_signature");
        }

        try {
            webhookService.processStripeEvent(event.getId(), event.getType(), payload);
        } catch (Exception e) {
            log.error("Falha ao processar webhook Stripe", e);
            return ResponseEntity.internalServerError().body("processing_error");
        }
        return ResponseEntity.ok("ok");
    }
}
