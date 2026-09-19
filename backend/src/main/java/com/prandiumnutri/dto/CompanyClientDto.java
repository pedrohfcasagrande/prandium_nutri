package com.prandiumnutri.dto;

import com.prandiumnutri.domain.LinkStatus;

import java.time.LocalDate;

/** Cliente + nutri atual dentro de uma empresa. Visao consolidada. */
public record CompanyClientDto(
        Long linkId,
        Long clientId,
        String clientName,
        String clientEmail,
        /** CPF mascarado do cliente (LGPD). */
        String clientCpf,
        Long nutriId,
        String nutriName,
        LinkStatus status,
        LocalDate lastMealDate
) {
    /** Overload compat (sem cpf) para caminhos que ainda nao passam pelo mascarador. */
    public CompanyClientDto(Long linkId, Long clientId, String clientName, String clientEmail,
                             Long nutriId, String nutriName, LinkStatus status, LocalDate lastMealDate) {
        this(linkId, clientId, clientName, clientEmail, null, nutriId, nutriName, status, lastMealDate);
    }
}
