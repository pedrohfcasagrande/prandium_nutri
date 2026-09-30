package com.prandiumnutri.service;

import com.prandiumnutri.domain.Notification;
import com.prandiumnutri.domain.NotificationType;
import com.prandiumnutri.repository.NotificationRepository;
import com.prandiumnutri.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final PushSender pushSender;

    /**
     * Grava a notificacao in-app e envia email para o mesmo user.
     * Nao dispara excecao se o email falhar — audit log fica ok.
     */
    @Transactional
    public Notification notify(Long userId, NotificationType type,
                                String title, String body,
                                String entityType, Long entityId) {
        Notification n = repository.save(Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .entityType(entityType)
                .entityId(entityId)
                .build());
        try {
            userRepository.findById(userId).ifPresent(u ->
                    mailService.send(u.getEmail(), title, body == null ? "" : body));
        } catch (Exception e) {
            log.warn("Falha ao enviar email da notificacao {}: {}", n.getId(), e.getMessage());
        }
        // Fase 9G-4: dispara push criptografado @Async — nao bloqueia o commit.
        try {
            pushSender.sendToUser(userId, title, body, entityType, entityId);
        } catch (Exception e) {
            log.warn("Falha ao disparar push da notificacao {}: {}", n.getId(), e.getMessage());
        }
        return n;
    }

    @Transactional(readOnly = true)
    public List<Notification> list(Long userId, boolean unreadOnly, int limit) {
        var page = PageRequest.of(0, Math.min(limit, 100));
        return unreadOnly
                ? repository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, page)
                : repository.findByUserIdOrderByCreatedAtDesc(userId, page);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        repository.findById(notificationId).ifPresent(n -> {
            if (!n.getUserId().equals(userId)) return;   // silencioso; nao vaza info
            if (n.getReadAt() != null) return;
            n.setReadAt(LocalDateTime.now());
            repository.save(n);
        });
    }

    @Transactional
    public int markAllRead(Long userId) {
        return repository.markAllRead(userId, LocalDateTime.now());
    }
}
