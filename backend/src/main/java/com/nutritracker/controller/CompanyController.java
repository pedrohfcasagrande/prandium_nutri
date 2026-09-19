package com.nutritracker.controller;

import com.nutritracker.dto.*;
import com.nutritracker.security.CurrentUser;
import com.nutritracker.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final CurrentUser currentUser;

    @GetMapping("/me")
    @PreAuthorize("hasRole('COMPANY')")
    public CompanyDto me() {
        return companyService.toDto(companyService.getByOwner(currentUser.getAuthenticatedId()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('COMPANY')")
    public CompanyDto update(@Valid @RequestBody CompanyDto dto) {
        return companyService.updateOwn(currentUser.getAuthenticated(), dto);
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('COMPANY')")
    public CompanyOverviewDto overview() {
        Long companyId = companyService.getByOwner(currentUser.getAuthenticatedId()).getId();
        return companyService.overview(companyId);
    }

    @GetMapping("/nutritionists")
    @PreAuthorize("hasRole('COMPANY')")
    public List<CompanyNutriLinkDto> nutritionists() {
        Long companyId = companyService.getByOwner(currentUser.getAuthenticatedId()).getId();
        return companyService.listNutritionists(companyId);
    }

    @PostMapping("/nutritionists/invite")
    @PreAuthorize("hasRole('COMPANY')")
    public CompanyNutriLinkDto invite(@RequestBody InviteClientRequest req) {
        return companyService.inviteNutri(
                companyService.getByOwner(currentUser.getAuthenticatedId()),
                req.clientEmail()   // reusa DTO com campo clientEmail (email do nutri, mesmo shape)
        );
    }

    @DeleteMapping("/nutritionists/{linkId}")
    @PreAuthorize("hasRole('COMPANY')")
    public void revoke(@PathVariable Long linkId) {
        companyService.revokeByCompany(
                companyService.getByOwner(currentUser.getAuthenticatedId()), linkId);
    }

    /** Revogar nutri escolhendo o que fazer com os clientes (REVOKE_CLIENTS ou TRANSFER). */
    @PostMapping("/nutritionists/{linkId}/revoke")
    @PreAuthorize("hasRole('COMPANY')")
    public void revokeWithAction(@PathVariable Long linkId,
                                  @RequestBody java.util.Map<String, Object> body) {
        String action = (String) body.getOrDefault("action", "REVOKE_CLIENTS");
        Long targetNutriId = body.get("targetNutriId") != null
                ? Long.valueOf(body.get("targetNutriId").toString()) : null;
        companyService.revokeWithAction(
                companyService.getByOwner(currentUser.getAuthenticatedId()),
                linkId, action, targetNutriId);
    }

    // --- Clientes agregados ---

    @GetMapping("/clients")
    @PreAuthorize("hasRole('COMPANY')")
    public java.util.List<com.nutritracker.dto.CompanyClientDto> clients() {
        Long companyId = companyService.getByOwner(currentUser.getAuthenticatedId()).getId();
        return companyService.listClients(companyId);
    }

    @PutMapping("/clients/{clientId}/nutri")
    @PreAuthorize("hasRole('COMPANY')")
    public void transferClient(@PathVariable Long clientId,
                                @RequestBody java.util.Map<String, Long> body) {
        Long target = body.get("targetNutriId");
        if (target == null) throw new IllegalArgumentException("targetNutriId obrigatorio");
        companyService.transferClient(
                companyService.getByOwner(currentUser.getAuthenticatedId()), clientId, target);
    }
}
