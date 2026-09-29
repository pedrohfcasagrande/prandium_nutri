package com.prandiumnutri.controller;

import com.prandiumnutri.domain.ClientAssessment;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.dto.AssessmentSnapshotDto;
import com.prandiumnutri.dto.ClientAssessmentDto;
import com.prandiumnutri.repository.ClientAssessmentRepository;
import com.prandiumnutri.repository.UserRepository;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.ClientAssessmentService;
import com.prandiumnutri.service.PrescriptionPdfService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nutritionist/assessments")
@RequiredArgsConstructor
public class NutriAssessmentController {

    private final ClientAssessmentService service;
    private final PrescriptionPdfService pdfService;
    private final ClientAssessmentRepository assessmentRepository;
    private final UserRepository userRepository;
    private final com.prandiumnutri.service.PrescriptionIssuanceService issuanceService;
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

    @PostMapping("/client/{clientId}/snapshots")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public AssessmentSnapshotDto createSnapshot(@PathVariable Long clientId,
                                                 @RequestParam(required = false) Long practiceId,
                                                 @RequestBody(required = false) Map<String, String> body) {
        String label = body != null ? body.get("label") : null;
        return service.createSnapshot(currentUser.getAuthenticated(), clientId, practiceId, label);
    }

    @GetMapping("/client/{clientId}/snapshots")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public List<AssessmentSnapshotDto> listSnapshots(@PathVariable Long clientId,
                                                      @RequestParam(required = false) Long practiceId) {
        return service.listSnapshots(currentUser.getAuthenticatedId(), clientId, practiceId);
    }

    /** Fase 9D-3: log de emissoes de PDF (hash SHA-256 + quem baixou) da ficha do cliente. */
    @GetMapping("/client/{clientId}/issuances")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public List<com.prandiumnutri.dto.PrescriptionIssuanceDto> listIssuances(
            @PathVariable Long clientId,
            @RequestParam(required = false) Long practiceId) {
        service.findByNutriAndClient(currentUser.getAuthenticatedId(), clientId, practiceId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Ficha nao encontrada"));
        var ficha = assessmentRepository.findByNutritionistIdAndClientId(
                currentUser.getAuthenticatedId(), clientId).orElseThrow();
        return issuanceService.listForAssessment(ficha.getId());
    }

    /** Fase 9D: PDF da prescricao. Nutri baixa a ficha do proprio cliente. */
    @GetMapping(value = "/client/{clientId}/prescription.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public ResponseEntity<byte[]> prescriptionPdf(@PathVariable Long clientId,
                                                   @RequestParam(required = false) Long practiceId) {
        User nutri = currentUser.getAuthenticated();
        // Reusa a autorizacao ja existente
        service.findByNutriAndClient(nutri.getId(), clientId, practiceId)
                .orElseThrow(() -> new EntityNotFoundException("Ficha nao encontrada"));
        ClientAssessment assessment = assessmentRepository
                .findByNutritionistIdAndClientId(nutri.getId(), clientId)
                .orElseThrow(() -> new EntityNotFoundException("Ficha nao encontrada"));
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente nao encontrado"));

        byte[] pdf = pdfService.render(assessment, client, nutri,
                LocaleContextHolder.getLocale(), /* snapshot */ true, "NUTRITIONIST");
        String hash = com.prandiumnutri.service.PrescriptionIssuanceService.sha256Hex(pdf);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=prescricao-" + clientId + ".pdf")
                .header("X-Pdf-Sha256", hash)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
