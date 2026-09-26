package com.prandiumnutri.controller;

import com.prandiumnutri.dto.AssessmentSelfFillDto;
import com.prandiumnutri.dto.AssessmentSnapshotDto;
import com.prandiumnutri.dto.ClientAssessmentDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.ClientAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Cliente le a propria ficha (mais recente) e pode complementar campos "safe"
 * (habitos, preferencias, objetivos). Antropometria/labs/prescricao ficam com o nutri.
 */
@RestController
@RequestMapping("/api/me/assessment")
@RequiredArgsConstructor
public class MyAssessmentController {

    private final ClientAssessmentService service;
    private final CurrentUser currentUser;

    @GetMapping
    public ResponseEntity<ClientAssessmentDto> get() {
        return service.findLatestForClient(currentUser.getAuthenticatedId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/self-fill")
    public ClientAssessmentDto selfFill(@RequestBody AssessmentSelfFillDto dto) {
        return service.selfFillForClient(currentUser.getAuthenticatedId(), dto);
    }

    @GetMapping("/snapshots")
    public List<AssessmentSnapshotDto> mySnapshots() {
        return service.listSnapshotsForClient(currentUser.getAuthenticatedId());
    }
}
