package com.nutritracker.controller;

import com.nutritracker.dto.CompanyInviteDto;
import com.nutritracker.dto.NutriCompanyDto;
import com.nutritracker.security.CurrentUser;
import com.nutritracker.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nutritionist")
@RequiredArgsConstructor
public class NutriCompanyController {

    private final CompanyService companyService;
    private final CurrentUser currentUser;

    @GetMapping("/companies")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public List<NutriCompanyDto> myCompanies() {
        return companyService.listCompaniesForNutri(currentUser.getAuthenticatedId());
    }

    @GetMapping("/company-invitations")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public List<CompanyInviteDto> pendingCompanyInvites() {
        return companyService.pendingCompanyInvitesFor(currentUser.getAuthenticated());
    }

    @PostMapping("/company-invitations/{token}/accept")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public void acceptCompany(@PathVariable String token) {
        companyService.acceptCompanyInvite(currentUser.getAuthenticated(), token);
    }

    @PostMapping("/company-invitations/{token}/reject")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public void rejectCompany(@PathVariable String token) {
        companyService.rejectCompanyInvite(currentUser.getAuthenticated(), token);
    }

    @DeleteMapping("/companies/{linkId}")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public void leaveCompany(@PathVariable Long linkId) {
        companyService.leaveCompany(currentUser.getAuthenticated(), linkId);
    }
}
