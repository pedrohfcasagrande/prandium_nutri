package com.prandiumnutri.controller;

import com.prandiumnutri.dto.ConsultationDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.ConsultationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nutritionist/consultations")
@RequiredArgsConstructor
public class NutriConsultationController {

    private final ConsultationService service;
    private final CurrentUser currentUser;

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public List<ConsultationDto> list(@PathVariable Long clientId,
                                       @RequestParam(required = false) Long practiceId) {
        return service.listForNutri(currentUser.getAuthenticated(), clientId, practiceId);
    }

    @PostMapping("/client/{clientId}")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public ConsultationDto create(@PathVariable Long clientId,
                                   @RequestParam(required = false) Long practiceId,
                                   @RequestParam(defaultValue = "true") boolean snapshot,
                                   @RequestBody ConsultationDto dto) {
        return service.create(currentUser.getAuthenticated(), clientId, practiceId, dto, snapshot);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public ConsultationDto update(@PathVariable Long id, @RequestBody ConsultationDto dto) {
        return service.update(currentUser.getAuthenticated(), id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public void delete(@PathVariable Long id) {
        service.delete(currentUser.getAuthenticated(), id);
    }
}
