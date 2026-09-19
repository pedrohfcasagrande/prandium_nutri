package com.prandiumnutri.controller;

import com.prandiumnutri.dto.AdminCompanyDto;
import com.prandiumnutri.dto.CreateCompanyRequest;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.CompanyOnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/companies")
@RequiredArgsConstructor
public class AdminCompanyController {

    private final CompanyOnboardingService onboardingService;
    private final CurrentUser currentUser;

    @GetMapping
    public List<AdminCompanyDto> list() {
        return onboardingService.listAll();
    }

    @GetMapping("/{id}")
    public AdminCompanyDto get(@PathVariable Long id) {
        return onboardingService.getById(id);
    }

    @PostMapping
    public AdminCompanyDto create(@Valid @RequestBody CreateCompanyRequest req) {
        return onboardingService.createByAdmin(currentUser.getAuthenticated(), req);
    }

    @PutMapping("/{id}/active")
    public AdminCompanyDto setActive(@PathVariable Long id, @RequestParam boolean value) {
        return onboardingService.setActive(id, value);
    }

    @PostMapping("/{id}/resend-owner-invite")
    public void resendInvite(@PathVariable Long id) {
        onboardingService.resendOwnerInvite(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        onboardingService.delete(id);
    }
}
