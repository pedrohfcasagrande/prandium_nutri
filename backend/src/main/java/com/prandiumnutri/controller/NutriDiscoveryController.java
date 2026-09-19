package com.prandiumnutri.controller;

import com.prandiumnutri.domain.NutritionistPractice;
import com.prandiumnutri.dto.ClientLinkDto;
import com.prandiumnutri.dto.NutriProfileUpdateDto;
import com.prandiumnutri.dto.NutriPublicDto;
import com.prandiumnutri.repository.NutritionistPracticeRepository;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.FeatureGateService;
import com.prandiumnutri.service.NutritionistLinkService;
import com.prandiumnutri.service.NutritionistPracticeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nutritionists")
@RequiredArgsConstructor
public class NutriDiscoveryController {

    private final NutritionistPracticeService practiceService;
    private final NutritionistPracticeRepository practiceRepository;
    private final CurrentUser currentUser;
    private final NutritionistLinkService linkService;
    private final FeatureGateService featureGate;

    /** Lista publica de practices ACTIVE + opt-in (uma linha por practice). */
    @GetMapping("/available")
    public List<NutriPublicDto> available() {
        return practiceService.availablePractices().stream().map(NutriPublicDto::from).toList();
    }

    /**
     * Perfil publico de uma practice especifica.
     * `id` = practiceId (Fase 5B). Chamadas antigas com userId caem no fallback
     * para a practice particular do nutri.
     */
    @GetMapping("/{id}")
    public NutriPublicDto getOne(@PathVariable Long id) {
        return practiceRepository.findById(id)
                .map(NutriPublicDto::from)
                .orElseGet(() -> practiceRepository.findParticular(id)
                        .map(NutriPublicDto::from)
                        .orElseThrow(() -> new EntityNotFoundException("Practice not found")));
    }

    /**
     * Nutri atualiza seu perfil publico. Nesta fase atualiza a practice particular
     * (comportamento equivalente ao pre-5B). Fase 5D adiciona seletor de practice.
     */
    @PutMapping("/me/profile")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    @Transactional
    public NutriPublicDto updateOwn(@RequestBody NutriProfileUpdateDto dto) {
        var nutri = currentUser.getAuthenticated();
        NutritionistPractice p = practiceService.ensureParticular(nutri);
        p.setAcceptsNewClients(dto.acceptsNewClients());
        p.setBio(dto.bio());
        p.setSpecialties(dto.specialties());
        // Mantem tambem no user por compat (Fase 5B ainda co-existe; sera limpo em fase futura)
        nutri.setAcceptsNewClients(dto.acceptsNewClients());
        nutri.setBio(dto.bio());
        nutri.setSpecialties(dto.specialties());
        return NutriPublicDto.from(p);
    }

    @GetMapping("/me/profile")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public NutriProfilePublicMe me() {
        var nutri = currentUser.getAuthenticated();
        NutritionistPractice p = practiceService.ensureParticular(nutri);
        return new NutriProfilePublicMe(p.isAcceptsNewClients(), p.getBio(), p.getSpecialties());
    }

    public record NutriProfilePublicMe(boolean acceptsNewClients, String bio, String specialties) {}

    /** Retorna se o cliente autenticado tem acesso a feature `findNutritionist` no plano. */
    @GetMapping("/can-find")
    public Map<String, Boolean> canFind() {
        boolean allowed = featureGate.canFindNutri(currentUser.getAuthenticatedId());
        return Map.of("allowed", allowed);
    }

    // --- Client-initiated request ---

    /**
     * Cliente pede vinculo. `id` = practiceId. O vinculo nutri-cliente e criado
     * apontando para o `nutritionist_id` da practice (a coluna `practice_id` na
     * NCL sera adicionada na Fase 5C).
     */
    @PostMapping("/{id}/request-link")
    public ClientLinkDto requestLink(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, String> body) {
        String message = body == null ? null : body.get("message");
        // Resolve practice → nutri user id. Fallback: se `id` for userId antigo, usa direto.
        Long nutriId = practiceRepository.findById(id)
                .map(p -> p.getNutritionist().getId())
                .orElse(id);
        return linkService.requestLink(currentUser.getAuthenticated(), nutriId, message);
    }
}
