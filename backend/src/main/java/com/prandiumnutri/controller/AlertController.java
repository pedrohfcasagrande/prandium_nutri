package com.prandiumnutri.controller;

import com.prandiumnutri.domain.Alert;
import com.prandiumnutri.domain.AlertScope;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.AlertService;
import com.prandiumnutri.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final CompanyService companyService;
    private final CurrentUser currentUser;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Alert> adminAlerts() {
        return alertService.listActive(AlertScope.ADMIN, null);
    }

    @GetMapping("/company")
    @PreAuthorize("hasRole('COMPANY')")
    public List<Alert> companyAlerts() {
        Long companyId = companyService.getByOwner(currentUser.getAuthenticatedId()).getId();
        return alertService.listActive(AlertScope.COMPANY, companyId);
    }

    @GetMapping("/nutritionist")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public List<Alert> nutritionistAlerts() {
        return alertService.listActive(AlertScope.NUTRITIONIST, currentUser.getAuthenticatedId());
    }

    @PostMapping("/{id}/dismiss")
    public void dismiss(@PathVariable Long id) {
        alertService.dismiss(id, currentUser.getAuthenticatedId());
    }

    @PostMapping("/evaluate-now")
    @PreAuthorize("hasRole('ADMIN')")
    public void trigger() {
        alertService.evaluateAllRules();
    }
}
