package com.prandiumnutri.dto;

import com.prandiumnutri.domain.LinkStatus;
import com.prandiumnutri.domain.SubscriptionStatus;

import java.time.LocalDateTime;

public record ClientLinkDto(
        Long id,
        Long clientId,
        String clientName,
        String clientEmail,
        LinkStatus status,
        LocalDateTime invitedAt,
        LocalDateTime acceptedAt,
        LocalDateTime revokedAt,
        SubscriptionStatus subscriptionStatus,
        String subscriptionPlanCode,
        String subscriptionPlanName,
        LocalDateTime subscriptionRenewsAt
) {
    public ClientLinkDto(Long id, Long clientId, String clientName, String clientEmail,
                         LinkStatus status, LocalDateTime invitedAt,
                         LocalDateTime acceptedAt, LocalDateTime revokedAt) {
        this(id, clientId, clientName, clientEmail, status, invitedAt, acceptedAt, revokedAt,
                null, null, null, null);
    }
}
