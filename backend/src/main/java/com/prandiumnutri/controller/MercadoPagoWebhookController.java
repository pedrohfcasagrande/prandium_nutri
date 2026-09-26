package com.prandiumnutri.controller;

import com.prandiumnutri.payment.MercadoPagoSignatureVerifier;
import com.prandiumnutri.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook do Mercado Pago. Formato de notificacao:
 * https://www.mercadopago.com.br/developers/pt/docs/your-integrations/notifications/webhooks
 */
@RestController
@RequestMapping("/api/webhooks/mercado-pago")
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoWebhookController {

    private final PaymentWebhookService webhookService;
    private final MercadoPagoSignatureVerifier signatureVerifier;

    @PostMapping
    public ResponseEntity<String> receive(
            @RequestBody String payload,
            @RequestParam(value = "data.id", required = false) String dataIdQuery,
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId) {

        String eventId = extractId(payload);
        String dataId = dataIdQuery != null ? dataIdQuery : eventId;

        if (!signatureVerifier.verify(xSignature, xRequestId, dataId)) {
            log.warn("Assinatura MP invalida (dataId={}, requestId={})", dataId, xRequestId);
            return ResponseEntity.badRequest().body("invalid_signature");
        }

        try {
            webhookService.processMercadoPagoEvent(eventId, payload);
        } catch (Exception e) {
            log.error("Falha ao processar webhook MP", e);
            return ResponseEntity.internalServerError().body("processing_error");
        }
        return ResponseEntity.ok("ok");
    }

    private String extractId(String payload) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(payload);
            String id = node.path("id").asText(null);
            if (id == null || id.isBlank()) id = node.path("data").path("id").asText(null);
            return id;
        } catch (Exception e) {
            return null;
        }
    }
}
