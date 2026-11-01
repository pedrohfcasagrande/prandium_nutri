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
    private final boolean allowUnsigned;

    public StripeWebhookController(PaymentWebhookService webhookService,
                                    @Value("${prandiumnutri.stripe.webhook-secret:}") String signingSecret,
                                    @Value("${prandiumnutri.stripe.allow-unsigned-webhooks:false}") boolean allowUnsigned) {
        this.webhookService = webhookService;
        this.signingSecret = signingSecret;
        this.allowUnsigned = allowUnsigned;
    }

    @PostMapping
    public ResponseEntity<String> receive(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        Event event;
        boolean secretConfigured = signingSecret != null && !signingSecret.isBlank();
        if (secretConfigured && (signature == null || signature.isBlank())) {
            log.warn("Stripe webhook rejeitado: header Stripe-Signature ausente");
            return ResponseEntity.badRequest().body("missing_signature");
        }
        if (!secretConfigured && !allowUnsigned) {
            log.error("Stripe webhook rejeitado: STRIPE_WEBHOOK_SECRET nao configurado e allow-unsigned-webhooks=false");
            return ResponseEntity.status(503).body("webhook_not_configured");
        }
        try {
            if (secretConfigured) {
                event = Webhook.constructEvent(payload, signature, signingSecret);
            } else {
                log.warn("Stripe webhook sem validacao de assinatura (apenas dev/test)");
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
