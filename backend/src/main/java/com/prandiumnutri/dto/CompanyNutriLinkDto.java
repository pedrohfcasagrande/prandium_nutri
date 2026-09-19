package com.prandiumnutri.dto;

import com.prandiumnutri.domain.LinkStatus;

import java.time.LocalDateTime;

public record CompanyNutriLinkDto(
        Long id,
        Long nutritionistId,
        String nutritionistName,
        String nutritionistEmail,
        /** CPF mascarado do nutri (LGPD). Null se convite ainda nao aceito. */
        String nutritionistCpf,
        /** CRN completo do nutri (nao PII sensivel). */
        String nutritionistCrn,
        LinkStatus status,
        LocalDateTime invitedAt,
        LocalDateTime acceptedAt,
        long clientsCount
) {
    /** Overload compat sem cpf/crn. */
    public CompanyNutriLinkDto(Long id, Long nutritionistId, String nutritionistName, String nutritionistEmail,
                                LinkStatus status, LocalDateTime invitedAt, LocalDateTime acceptedAt, long clientsCount) {
        this(id, nutritionistId, nutritionistName, nutritionistEmail, null, null,
                status, invitedAt, acceptedAt, clientsCount);
    }
}
