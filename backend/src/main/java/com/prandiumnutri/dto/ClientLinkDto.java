package com.prandiumnutri.dto;

import com.prandiumnutri.domain.LinkStatus;
import com.prandiumnutri.domain.SubscriptionStatus;

import java.time.LocalDateTime;

public record ClientLinkDto(
        Long id,
        Long clientId,
        String clientName,
        String clientEmail,
        /** CPF mascarado do cliente (LGPD). Null se convite ainda nao aceito. */
        String clientCpf,
        LinkStatus status,
        LocalDateTime invitedAt,
        LocalDateTime acceptedAt,
        LocalDateTime revokedAt,
        SubscriptionStatus subscriptionStatus,
        String subscriptionPlanCode,
        String subscriptionPlanName,
        LocalDateTime subscriptionRenewsAt
) {
    /** Overload compat sem CPF nem subscription (para invite/accept response). */
    public ClientLinkDto(Long id, Long clientId, String clientName, String clientEmail,
                         LinkStatus status, LocalDateTime invitedAt,
                         LocalDateTime acceptedAt, LocalDateTime revokedAt) {
        this(id, clientId, clientName, clientEmail, null, status, invitedAt, acceptedAt, revokedAt,
                null, null, null, null);
    }
}
