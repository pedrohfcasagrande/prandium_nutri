package com.prandiumnutri.payment;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class MercadoPagoSignatureVerifierTest {

    private static final String SECRET = "test_secret_abc123";
    private static final String DATA_ID = "12345";
    private static final String REQUEST_ID = "req-abc";
    private static final long TS = 1735689600L;

    @Test
    void failOpen_whenSecretNotConfigured() {
        var verifier = new MercadoPagoSignatureVerifier("");
        assertThat(verifier.isConfigured()).isFalse();
        assertThat(verifier.verify(null, null, DATA_ID)).isTrue();
        assertThat(verifier.verify("garbage", null, DATA_ID)).isTrue();
    }

    @Test
    void rejects_whenSignatureHeaderMissing() {
        var verifier = new MercadoPagoSignatureVerifier(SECRET);
        assertThat(verifier.verify(null, REQUEST_ID, DATA_ID)).isFalse();
        assertThat(verifier.verify("", REQUEST_ID, DATA_ID)).isFalse();
    }

    @Test
    void rejects_whenDataIdMissing() {
        var verifier = new MercadoPagoSignatureVerifier(SECRET);
        String header = validHeader(SECRET, DATA_ID, REQUEST_ID, TS);
        assertThat(verifier.verify(header, REQUEST_ID, null)).isFalse();
        assertThat(verifier.verify(header, REQUEST_ID, "")).isFalse();
    }

    @Test
    void rejects_whenSignatureWrong() {
        var verifier = new MercadoPagoSignatureVerifier(SECRET);
        String bad = "ts=" + TS + ",v1=deadbeef";
        assertThat(verifier.verify(bad, REQUEST_ID, DATA_ID)).isFalse();
    }

    @Test
    void accepts_whenSignatureValid() {
        var verifier = new MercadoPagoSignatureVerifier(SECRET);
        String header = validHeader(SECRET, DATA_ID, REQUEST_ID, TS);
        assertThat(verifier.verify(header, REQUEST_ID, DATA_ID)).isTrue();
    }

    @Test
    void accepts_evenWithoutRequestId() {
        // MP omite request-id em alguns casos; manifest ainda deve bater
        var verifier = new MercadoPagoSignatureVerifier(SECRET);
        String header = validHeader(SECRET, DATA_ID, null, TS);
        assertThat(verifier.verify(header, null, DATA_ID)).isTrue();
    }

    // Constroi header valido pra dados de teste (mesmo algoritmo do verifier).
    private static String validHeader(String secret, String dataId, String requestId, long ts) {
        String manifest = "id:" + dataId + ";"
                + (requestId != null && !requestId.isBlank() ? "request-id:" + requestId + ";" : "")
                + "ts:" + ts + ";";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            return "ts=" + ts + ",v1=" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
