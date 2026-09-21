package com.prandiumnutri.controller;

import com.prandiumnutri.dto.ClientAssessmentDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.ClientAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nutritionist/assessments")
@RequiredArgsConstructor
public class NutriAssessmentController {

    private final ClientAssessmentService service;
    private final CurrentUser currentUser;

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public ResponseEntity<ClientAssessmentDto> getForClient(@PathVariable Long clientId,
                                                             @RequestParam(required = false) Long practiceId) {
        return service.findByNutriAndClient(currentUser.getAuthenticatedId(), clientId, practiceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/client/{clientId}")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public ClientAssessmentDto upsert(@PathVariable Long clientId,
                                       @RequestParam(required = false) Long practiceId,
                                       @RequestBody ClientAssessmentDto dto) {
        return service.upsert(currentUser.getAuthenticated(), clientId, practiceId, dto);
    }
}
