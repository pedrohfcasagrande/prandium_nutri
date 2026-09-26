package com.prandiumnutri.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Valida assinatura HMAC-SHA256 dos webhooks do Mercado Pago.
 * Docs: https://www.mercadopago.com.br/developers/pt/docs/your-integrations/notifications/webhooks#validate-origin
 *
 * Header x-signature: "ts=<timestamp>,v1=<hmac>"
 * Manifest assinado: "id:{dataId};request-id:{requestId};ts:{ts};"
 */
@Component
@Slf4j
public class MercadoPagoSignatureVerifier {

    private final String secret;

    public MercadoPagoSignatureVerifier(@Value("${prandiumnutri.mercadopago.webhook-secret:}") String secret) {
        this.secret = secret;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(secret);
    }

    /**
     * @return true se a assinatura for valida OU se o secret nao esta configurado
     *         (fail-open em dev/testes; em prod configure sempre o secret).
     */
    public boolean verify(String xSignature, String xRequestId, String dataId) {
        if (!isConfigured()) {
            log.warn("MP webhook sem verificacao (prandiumnutri.mercadopago.webhook-secret vazio)");
            return true;
        }
        if (!StringUtils.hasText(xSignature) || !StringUtils.hasText(dataId)) {
            return false;
        }

        String ts = null;
        String v1 = null;
        for (String part : xSignature.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) continue;
            if ("ts".equals(kv[0])) ts = kv[1];
            else if ("v1".equals(kv[0])) v1 = kv[1];
        }
        if (ts == null || v1 == null) return false;

        String manifest = "id:" + dataId + ";"
                + (StringUtils.hasText(xRequestId) ? "request-id:" + xRequestId + ";" : "")
                + "ts:" + ts + ";";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return constantTimeEquals(computed, v1);
        } catch (Exception e) {
            log.error("Falha ao computar HMAC do MP webhook: {}", e.getMessage());
            return false;
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
        return diff == 0;
    }
}
