package com.prandiumnutri.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prandiumnutri.domain.PushSubscription;
import com.prandiumnutri.repository.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Fase 9G-4: envia push criptografado para cada subscription do user.
 * <p>Fan-out best-effort: se um endpoint retorna 404/410 ("Gone"), remove a row.
 * Outras falhas viram warning e seguem. Execucao {@code @Async} para nao
 * bloquear o commit da notificacao (chamado pelo {@link NotificationService}).
 * <p>Se {@link PushService} nao foi criado (VAPID vazio), no-op.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushSender {

    private final PushSubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;

    /** Pode ser null se VAPID nao configurado — checar antes de usar. */
    @Autowired(required = false)
    private PushService pushService;

    @Value("${prandiumnutri.push.frontend-url:http://localhost:4201}")
    private String frontendUrl;

    @Async
    public void sendToUser(Long userId, String title, String body,
                            String entityType, Long entityId) {
        if (pushService == null) return;
        List<PushSubscription> subs = subscriptionRepository.findByUserId(userId);
        if (subs.isEmpty()) return;
        String payload = buildPayload(title, body, entityType, entityId);
        for (PushSubscription sub : subs) {
            trySend(sub, payload);
        }
    }

    private void trySend(PushSubscription sub, String payload) {
        try {
            Notification n = new Notification(sub.getEndpoint(), sub.getP256dh(), sub.getAuth(), payload);
            var response = pushService.send(n);
            int status = response.getStatusLine().getStatusCode();
            if (status == 404 || status == 410) {
                log.info("Push endpoint gone ({}), removendo subscription {}", status, sub.getId());
                subscriptionRepository.deleteById(sub.getId());
            } else if (status >= 400) {
                log.warn("Push falhou ({}): {}", status, sub.getEndpoint());
            }
        } catch (Exception e) {
            log.warn("Erro ao enviar push para {}: {}", sub.getEndpoint(), e.getMessage());
        }
    }

    private String buildPayload(String title, String body,
                                 String entityType, Long entityId) {
        try {
            java.util.HashMap<String, Object> map = new java.util.HashMap<>();
            map.put("title", title == null ? "" : title);
            map.put("body", body == null ? "" : body);
            map.put("frontendUrl", frontendUrl);
            if (entityType != null) map.put("entityType", entityType);
            if (entityId != null) map.put("entityId", entityId);
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
