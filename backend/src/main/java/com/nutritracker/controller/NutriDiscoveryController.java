package com.nutritracker.controller;

import com.nutritracker.dto.ClientLinkDto;
import com.nutritracker.dto.NutriPublicDto;
import com.nutritracker.dto.NutriProfileUpdateDto;
import com.nutritracker.repository.UserRepository;
import com.nutritracker.security.CurrentUser;
import com.nutritracker.service.NutritionistLinkService;
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

    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final NutritionistLinkService linkService;
    private final com.nutritracker.service.FeatureGateService featureGate;

    /** Lista publica (para clientes) dos nutris opt-in. */
    @GetMapping("/available")
    public List<NutriPublicDto> available() {
        return userRepository.findAvailableNutris().stream().map(NutriPublicDto::from).toList();
    }

    /** Perfil publico de um nutri especifico. */
    @GetMapping("/{id}")
    public NutriPublicDto getOne(@PathVariable Long id) {
        return userRepository.findById(id)
                .filter(u -> u.getRole() == com.nutritracker.domain.Role.NUTRITIONIST)
                .map(NutriPublicDto::from)
                .orElseThrow(() -> new EntityNotFoundException("Nutritionist not found"));
    }

    /** Nutri atualiza seu proprio perfil publico. */
    @PutMapping("/me/profile")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    @Transactional
    public NutriPublicDto updateOwn(@RequestBody NutriProfileUpdateDto dto) {
        var nutri = currentUser.getAuthenticated();
        nutri.setAcceptsNewClients(dto.acceptsNewClients());
        nutri.setBio(dto.bio());
        nutri.setSpecialties(dto.specialties());
        return NutriPublicDto.from(userRepository.save(nutri));
    }

    @GetMapping("/me/profile")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public NutriProfilePublicMe me() {
        var u = currentUser.getAuthenticated();
        return new NutriProfilePublicMe(u.isAcceptsNewClients(), u.getBio(), u.getSpecialties());
    }

    public record NutriProfilePublicMe(boolean acceptsNewClients, String bio, String specialties) {}

    /** Retorna se o cliente autenticado tem acesso a feature `findNutritionist` no plano. */
    @GetMapping("/can-find")
    public Map<String, Boolean> canFind() {
        boolean allowed = featureGate.canFindNutri(currentUser.getAuthenticatedId());
        return Map.of("allowed", allowed);
    }

    // --- Client-initiated request ---

    @PostMapping("/{id}/request-link")
    public ClientLinkDto requestLink(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, String> body) {
        String message = body == null ? null : body.get("message");
        return linkService.requestLink(currentUser.getAuthenticated(), id, message);
    }
}

