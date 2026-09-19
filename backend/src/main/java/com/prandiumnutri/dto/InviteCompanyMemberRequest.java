package com.prandiumnutri.dto;

import com.prandiumnutri.domain.CompanyMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteCompanyMemberRequest(
        @NotBlank @Email String email,
        @NotNull CompanyMemberRole role
) {}
