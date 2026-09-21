package com.prandiumnutri.controller;

import com.prandiumnutri.dto.MenuPlanDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.MenuPlanService;
import jakarta.validation.Valid;
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
    public List<MenuPlanDto> pending(@RequestParam(required = false) Long practiceId) {
        return service.listPendingForNutri(currentUser.getAuthenticatedId(), practiceId);
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

    // ---- Authoring: nutri cria/edita cardapios em nome do cliente ----

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public List<MenuPlanDto> listForClient(@PathVariable Long clientId,
                                           @RequestParam(required = false) Long practiceId) {
        return service.listForClientByNutri(currentUser.getAuthenticatedId(), clientId, practiceId);
    }

    @PostMapping("/for/{clientId}")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public MenuPlanDto createForClient(@PathVariable Long clientId,
                                        @RequestParam(required = false) Long practiceId,
                                        @Valid @RequestBody MenuPlanDto dto) {
        return service.createForClient(currentUser.getAuthenticated(), clientId, practiceId, dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public MenuPlanDto updatePlan(@PathVariable Long id,
                                   @RequestParam(required = false) Long practiceId,
                                   @Valid @RequestBody MenuPlanDto dto) {
        return service.updateForClient(currentUser.getAuthenticated(), id, practiceId, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public void deletePlan(@PathVariable Long id,
                            @RequestParam(required = false) Long practiceId) {
        service.deleteForClient(currentUser.getAuthenticated(), id, practiceId);
    }
}
