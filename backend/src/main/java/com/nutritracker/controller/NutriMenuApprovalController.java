package com.nutritracker.controller;

import com.nutritracker.dto.MenuPlanDto;
import com.nutritracker.security.CurrentUser;
import com.nutritracker.service.MenuPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nutritionist/menu-plans")
@RequiredArgsConstructor
public class NutriMenuApprovalController {

    private final MenuPlanService service;
    private final CurrentUser currentUser;

    @GetMapping("/pending")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public List<MenuPlanDto> pending() {
        return service.listPendingForNutri(currentUser.getAuthenticatedId());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public MenuPlanDto approve(@PathVariable Long id) {
        return service.approve(currentUser.getAuthenticated(), id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public MenuPlanDto reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return service.reject(currentUser.getAuthenticated(), id, reason);
    }
}
