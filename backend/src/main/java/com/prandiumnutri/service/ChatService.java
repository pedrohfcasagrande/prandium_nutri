package com.prandiumnutri.service;

import com.prandiumnutri.domain.ChatMessage;
import com.prandiumnutri.domain.LinkStatus;
import com.prandiumnutri.domain.NotificationType;
import com.prandiumnutri.domain.NutritionistClientLink;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.dto.ChatMessageDto;
import com.prandiumnutri.repository.ChatMessageRepository;
import com.prandiumnutri.repository.NutritionistClientLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Fase 9G-1: chat cliente ↔ nutri.
 * <p>Uma thread por {@link NutritionistClientLink} ACTIVE. Autorizacao piggyback
 * do link (se nao ACTIVE ou requester nao e um dos dois lados, 403).
 * <p>SSE (Server-Sent Events) leve: cada linkId mantem uma lista de emitters
 * em memoria. Broadcast so pra participantes daquele link. Sem sticky sessions
 * pra scale-out — em prod multi-instance, migrar pra Redis pub/sub (backlog).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30 min

    private final ChatMessageRepository chatRepository;
    private final NutritionistClientLinkRepository linkRepository;
    private final NotificationService notificationService;

    /** Emitters ativos por linkId. Lista thread-safe. */
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Transactional
    public ChatMessageDto send(User sender, Long linkId, String content) {
        NutritionistClientLink link = authorized(linkId, sender.getId());
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Mensagem vazia");
        }
        String trimmed = content.trim();
        if (trimmed.length() > 2000) {
            throw new IllegalArgumentException("Mensagem muito longa (max 2000)");
        }
        ChatMessage saved = chatRepository.save(ChatMessage.builder()
                .link(link)
                .sender(sender)
                .content(trimmed)
                .createdAt(LocalDateTime.now())
                .build());

        ChatMessageDto dto = toDto(saved);
        broadcast(linkId, dto);

        Long recipientId = link.getNutritionist().getId().equals(sender.getId())
                ? link.getClient().getId()
                : link.getNutritionist().getId();
        String preview = trimmed.length() > 80 ? trimmed.substring(0, 80) + "…" : trimmed;
        notificationService.notify(recipientId, NotificationType.GENERIC,
                "Nova mensagem de " + sender.getName(),
                preview,
                "Chat", linkId);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> list(User requester, Long linkId) {
        authorized(linkId, requester.getId());
        return chatRepository.findByLinkIdOrderByCreatedAtAsc(linkId).stream()
                .map(this::toDto).toList();
    }

    @Transactional
    public int markRead(User reader, Long linkId) {
        authorized(linkId, reader.getId());
        return chatRepository.markReadForReader(linkId, reader.getId(), LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public long unreadCount(User reader, Long linkId) {
        authorized(linkId, reader.getId());
        return chatRepository.countUnreadForReader(linkId, reader.getId());
    }

    @Transactional(readOnly = true)
    public long unreadTotal(Long userId) {
        return chatRepository.countUnreadTotalForUser(userId);
    }

    // ---------- SSE ----------

    public SseEmitter subscribe(User user, Long linkId) {
        authorized(linkId, user.getId());
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.computeIfAbsent(linkId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(linkId, emitter));
        emitter.onTimeout(() -> remove(linkId, emitter));
        emitter.onError(t -> remove(linkId, emitter));

        try {
            emitter.send(SseEmitter.event().name("ready").data("ok"));
        } catch (Exception e) {
            remove(linkId, emitter);
        }
        return emitter;
    }

    private void broadcast(Long linkId, ChatMessageDto dto) {
        List<SseEmitter> subs = emitters.get(linkId);
        if (subs == null || subs.isEmpty()) return;
        for (SseEmitter e : subs) {
            try {
                e.send(SseEmitter.event().name("message").data(dto));
            } catch (Exception ex) {
                remove(linkId, e);
            }
        }
    }

    private void remove(Long linkId, SseEmitter emitter) {
        List<SseEmitter> subs = emitters.get(linkId);
        if (subs != null) subs.remove(emitter);
    }

    // ---------- helpers ----------

    private NutritionistClientLink authorized(Long linkId, Long userId) {
        NutritionistClientLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new AccessDeniedException("Vinculo nao encontrado"));
        if (link.getStatus() != LinkStatus.ACTIVE) {
            throw new AccessDeniedException("Vinculo nao esta ativo");
        }
        boolean isParticipant = link.getNutritionist().getId().equals(userId)
                              || link.getClient().getId().equals(userId);
        if (!isParticipant) {
            throw new AccessDeniedException("Sem permissao para este chat");
        }
        return link;
    }

    private ChatMessageDto toDto(ChatMessage m) {
        return new ChatMessageDto(
                m.getId(),
                m.getLink().getId(),
                m.getSender().getId(),
                m.getSender().getName(),
                m.getContent(),
                m.getCreatedAt(),
                m.getReadAt()
        );
    }
}
