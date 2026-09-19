package com.prandiumnutri.service;

import com.prandiumnutri.domain.AuditAction;
import com.prandiumnutri.domain.AuditLog;
import com.prandiumnutri.domain.Role;
import com.prandiumnutri.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;
    private final ApplicationEventPublisher publisher;

    public void record(AuditAction action,
                       Long actorUserId,
                       Role actorRole,
                       Long onBehalfOfUserId,
                       Long companyContextId,
                       String entityType,
                       Long entityId,
                       String metadata) {
        publisher.publishEvent(new AuditEvent(
                action, actorUserId, actorRole, onBehalfOfUserId,
                companyContextId, entityType, entityId, metadata));
    }

    public void record(AuditAction action, Long actorUserId, Role actorRole) {
        record(action, actorUserId, actorRole, null, null, null, null, null);
    }

    public void record(AuditAction action, Long actorUserId, Role actorRole, String entityType, Long entityId) {
        record(action, actorUserId, actorRole, null, null, entityType, entityId, null);
    }

    /**
     * Grava em nova tx sempre — nao herda readonly da tx do caller.
     * @Async garante execucao fora do request thread.
     */
    @Async("auditExecutor")
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(AuditEvent event) {
        try {
            repository.save(AuditLog.builder()
                    .action(event.action())
                    .actorUserId(event.actorUserId())
                    .actorRole(event.actorRole())
                    .onBehalfOfUserId(event.onBehalfOfUserId())
                    .companyContextId(event.companyContextId())
                    .entityType(event.entityType())
                    .entityId(event.entityId())
                    .metadata(event.metadata())
                    .build());
        } catch (Exception e) {
            log.warn("Falha ao gravar audit log ({}): {}", event.action(), e.getMessage());
        }
    }

    public record AuditEvent(
            AuditAction action,
            Long actorUserId,
            Role actorRole,
            Long onBehalfOfUserId,
            Long companyContextId,
            String entityType,
            Long entityId,
            String metadata
    ) {}
}
