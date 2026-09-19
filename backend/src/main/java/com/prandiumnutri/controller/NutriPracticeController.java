package com.prandiumnutri.controller;

import com.prandiumnutri.domain.NutritionistPractice;
import com.prandiumnutri.dto.NutriProfileUpdateDto;
import com.prandiumnutri.dto.PracticeDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.NutritionistPracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Fase 5D: gestao dos perfis de trabalho do nutricionista (particular + vinculadas).
 */
@RestController
@RequestMapping("/api/nutritionist/practices")
@RequiredArgsConstructor
public class NutriPracticeController {

    private final NutritionistPracticeService practiceService;
    private final CurrentUser currentUser;

    @GetMapping
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public List<PracticeDto> myPractices() {
        return practiceService.listByNutri(currentUser.getAuthenticatedId()).stream()
                .map(PracticeDto::from).toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    @Transactional
    public PracticeDto update(@PathVariable Long id, @RequestBody NutriProfileUpdateDto dto) {
        NutritionistPractice p = practiceService.getOrThrow(id);
        if (!p.getNutritionist().getId().equals(currentUser.getAuthenticatedId())) {
            throw new AccessDeniedException("Esta practice nao pertence a voce");
        }
        p.setBio(dto.bio());
        p.setSpecialties(dto.specialties());
        p.setAcceptsNewClients(dto.acceptsNewClients());
        return PracticeDto.from(p);
    }
}
