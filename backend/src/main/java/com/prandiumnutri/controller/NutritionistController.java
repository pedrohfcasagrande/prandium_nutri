package com.prandiumnutri.controller;

import com.prandiumnutri.domain.User;
import com.prandiumnutri.dto.ClientLinkDto;
import com.prandiumnutri.dto.InviteClientRequest;
import com.prandiumnutri.repository.UserRepository;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.NutritionistLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nutritionist")
@RequiredArgsConstructor
public class NutritionistController {

    private final NutritionistLinkService linkService;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    @GetMapping("/clients")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public com.prandiumnutri.dto.PageDto<ClientLinkDto> myClients(
            @RequestParam(required = false) Long practiceId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return com.prandiumnutri.dto.PageDto.of(
                linkService.listClientsForNutri(currentUser.getAuthenticatedId(), practiceId, q, sort),
                page, size);
    }

    @PostMapping("/clients/invite")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public ClientLinkDto invite(@Valid @RequestBody InviteClientRequest req,
                                @RequestParam(required = false) Long practiceId) {
        return linkService.invite(currentUser.getAuthenticated(), req.clientEmail(), practiceId);
    }

    @DeleteMapping("/clients/{linkId}")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public void revoke(@PathVariable Long linkId) {
        linkService.revokeByNutri(currentUser.getAuthenticated(), linkId);
    }

    // 4D: pedidos iniciados por clientes

    @GetMapping("/client-requests")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public List<ClientLinkDto> requests(@RequestParam(required = false) Long practiceId) {
        return linkService.listClientRequests(currentUser.getAuthenticatedId(), practiceId);
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

    // Fase 9D: assinatura digital para rodape do PDF de prescricao

    @GetMapping("/me/signature")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public Map<String, String> getSignature() {
        User u = currentUser.getAuthenticated();
        return Map.of("signatureBase64", u.getSignatureBase64() != null ? u.getSignatureBase64() : "");
    }

    @PutMapping("/me/signature")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public void putSignature(@RequestBody Map<String, String> body) {
        String base64 = body.get("signatureBase64");
        // Limite defensivo: ~2MB em base64 (~1.5MB de imagem). Acima disso, rejeita.
        if (base64 != null && base64.length() > 2_800_000) {
            throw new IllegalArgumentException("Assinatura muito grande (> 2MB)");
        }
        User u = currentUser.getAuthenticated();
        u.setSignatureBase64(base64 != null && !base64.isBlank() ? base64 : null);
        userRepository.save(u);
    }
}
