package com.nutritracker.controller;

import com.nutritracker.dto.ClientLinkDto;
import com.nutritracker.dto.InviteClientRequest;
import com.nutritracker.security.CurrentUser;
import com.nutritracker.service.NutritionistLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nutritionist")
@RequiredArgsConstructor
public class NutritionistController {

    private final NutritionistLinkService linkService;
    private final CurrentUser currentUser;

    @GetMapping("/clients")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public List<ClientLinkDto> myClients() {
        return linkService.listClientsForNutri(currentUser.getAuthenticatedId());
    }

    @PostMapping("/clients/invite")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public ClientLinkDto invite(@Valid @RequestBody InviteClientRequest req) {
        return linkService.invite(currentUser.getAuthenticated(), req.clientEmail());
    }

    @DeleteMapping("/clients/{linkId}")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public void revoke(@PathVariable Long linkId) {
        linkService.revokeByNutri(currentUser.getAuthenticated(), linkId);
    }

    // 4D: pedidos iniciados por clientes

    @GetMapping("/client-requests")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public List<ClientLinkDto> requests() {
        return linkService.listClientRequests(currentUser.getAuthenticatedId());
    }

    @PostMapping("/client-requests/{linkId}/accept")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public void acceptRequest(@PathVariable Long linkId) {
        linkService.acceptRequest(currentUser.getAuthenticated(), linkId);
    }

    @PostMapping("/client-requests/{linkId}/reject")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public void rejectRequest(@PathVariable Long linkId,
                               @RequestBody(required = false) java.util.Map<String, String> body) {
        String note = body == null ? null : body.get("note");
        linkService.rejectRequest(currentUser.getAuthenticated(), linkId, note);
    }
}
