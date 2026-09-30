package com.prandiumnutri.dto;

import java.time.LocalDateTime;

/**
 * Fase 9G-2 refino: item da lista do chat launcher.
 * Backend agrega link + counterparty + unread + preview em uma unica chamada
 * pro popover render em tela sem N requests.
 */
public record ConversationSummaryDto(
        Long linkId,
        Long counterpartyId,
        String counterpartyName,
        String counterpartyRole,       // "USER" | "NUTRITIONIST"
        boolean counterpartyOnline,    // last_seen_at < 2 min
        Long practiceId,
        String practiceLabel,
        Long companyId,
        long unreadCount,
        String lastMessagePreview,     // trimmed 80 chars, null se sem historico
        LocalDateTime lastMessageAt
) {}
