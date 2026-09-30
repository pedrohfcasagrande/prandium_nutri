package com.prandiumnutri.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.PushService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.Security;

/**
 * Fase 9G-4: config das chaves VAPID pra envio de push criptografado.
 * <p>Gerar par (uma vez): {@code npx web-push generate-vapid-keys} ou
 * {@code openssl ecparam -genkey -name prime256v1 -noout -out vapid.pem}.
 * <p>Se as chaves nao vierem definidas, o bean {@link PushService} nao e criado —
 * {@code PushSender} detecta a ausencia e no-opa (log warn).
 */
@Configuration
@Slf4j
public class WebPushConfig {

    @Getter
    @Value("${prandiumnutri.push.vapid-public-key:}")
    private String vapidPublicKey;

    @Value("${prandiumnutri.push.vapid-private-key:}")
    private String vapidPrivateKey;

    @Value("${prandiumnutri.push.vapid-subject:mailto:admin@prandiumnutri.local}")
    private String vapidSubject;

    @Bean
    public PushService pushService() {
        if (vapidPublicKey.isBlank() || vapidPrivateKey.isBlank()) {
            log.warn("VAPID keys vazias — push notifications desabilitadas. "
                    + "Gere um par (npx web-push generate-vapid-keys) e defina "
                    + "PRANDIUMNUTRI_PUSH_VAPID_PUBLIC_KEY / _PRIVATE_KEY.");
            return null;
        }
        try {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            PushService svc = new PushService();
            svc.setPublicKey(vapidPublicKey);
            svc.setPrivateKey(vapidPrivateKey);
            svc.setSubject(vapidSubject);
            return svc;
        } catch (Exception e) {
            log.error("Falha ao inicializar PushService: {}", e.getMessage(), e);
            return null;
        }
    }
}
