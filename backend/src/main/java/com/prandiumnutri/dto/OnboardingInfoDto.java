package com.prandiumnutri.dto;

import com.prandiumnutri.domain.CompanyMemberRole;

public record OnboardingInfoDto(
        Long companyId,
        String companyName,
        String invitedEmail,
        CompanyMemberRole role,
        boolean userExists   // se true, so precisa aceitar (nao precisa criar conta)
) {}
