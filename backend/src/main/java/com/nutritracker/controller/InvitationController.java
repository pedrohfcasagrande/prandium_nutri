package com.nutritracker.controller;

import com.nutritracker.dto.NutritionistLinkDto;
import com.nutritracker.dto.PendingInviteDto;
import com.nutritracker.security.CurrentUser;
import com.nutritracker.service.NutritionistLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class InvitationController {

    private final NutritionistLinkService linkService;
    private final CurrentUser currentUser;

    @GetMapping("/nutritionists")
    public List<NutritionistLinkDto> myNutritionists() {
        return linkService.listNutritionistsForClient(currentUser.getAuthenticatedId());
    }

    @DeleteMapping("/nutritionists/{linkId}")
    public void revoke(@PathVariable Long linkId) {
        linkService.revokeByClient(currentUser.getAuthenticated(), linkId);
    }

    @GetMapping("/invitations")
    public List<PendingInviteDto> pending() {
        return linkService.pendingInvitesFor(currentUser.getAuthenticated());
    }

    @PostMapping("/invitations/{token}/accept")
    public void accept(@PathVariable String token) {
        linkService.acceptInvite(currentUser.getAuthenticated(), token);
    }

    @PostMapping("/invitations/{token}/reject")
    public void reject(@PathVariable String token) {
        linkService.rejectInvite(currentUser.getAuthenticated(), token);
    }
}
