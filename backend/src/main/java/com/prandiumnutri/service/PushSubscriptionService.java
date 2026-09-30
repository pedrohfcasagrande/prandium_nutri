package com.prandiumnutri.service;

import com.prandiumnutri.domain.PushSubscription;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.dto.PushSubscriptionDto;
import com.prandiumnutri.repository.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fase 9G-4: gerenciamento das subscriptions Web Push por user.
 * <p>Upsert por {@code endpoint}: se ja existe (mesmo browser reregistrando),
 * atualiza dono/chaves. Se novo, insere. {@link PushSender} usa {@link #listForUser}
 * para fan-out em cada notificacao.
 */
@Service
@RequiredArgsConstructor
public class PushSubscriptionService {

    private final PushSubscriptionRepository repository;

    @Transactional
    public PushSubscription subscribe(User user, PushSubscriptionDto dto) {
        if (dto.endpoint() == null || dto.p256dh() == null || dto.auth() == null) {
            throw new IllegalArgumentException("Subscription incompleta");
        }
        PushSubscription entity = repository.findByEndpoint(dto.endpoint())
                .orElseGet(() -> PushSubscription.builder()
                        .endpoint(dto.endpoint())
                        .createdAt(LocalDateTime.now())
                        .build());
        entity.setUser(user);
        entity.setP256dh(dto.p256dh());
        entity.setAuth(dto.auth());
        entity.setUserAgent(dto.userAgent());
        return repository.save(entity);
    }

    @Transactional
    public void unsubscribe(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) return;
        repository.deleteByEndpoint(endpoint);
    }

    @Transactional(readOnly = true)
    public List<PushSubscription> listForUser(Long userId) {
        return repository.findByUserId(userId);
    }
}
