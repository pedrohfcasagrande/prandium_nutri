package com.nutritracker.controller;

import com.nutritracker.dto.CompanyMemberDto;
import com.nutritracker.dto.InviteCompanyMemberRequest;
import com.nutritracker.security.CurrentUser;
import com.nutritracker.service.CompanyOnboardingService;
import com.nutritracker.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company/members")
@RequiredArgsConstructor
public class CompanyMemberController {

    private final CompanyOnboardingService onboardingService;
    private final CompanyService companyService;
    private final CurrentUser currentUser;

    @GetMapping
    @PreAuthorize("hasRole('COMPANY')")
    public List<CompanyMemberDto> list() {
        Long companyId = companyService.getByOwner(currentUser.getAuthenticatedId()).getId();
        return onboardingService.listMembers(companyId);
    }

    @PostMapping("/invite")
    @PreAuthorize("hasRole('COMPANY')")
    public CompanyMemberDto invite(@Valid @RequestBody InviteCompanyMemberRequest req) {
        var company = companyService.getByOwner(currentUser.getAuthenticatedId());
        return onboardingService.inviteMember(company, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COMPANY')")
    public void revoke(@PathVariable Long id) {
        var company = companyService.getByOwner(currentUser.getAuthenticatedId());
        onboardingService.revokeMember(company, id);
    }
}
