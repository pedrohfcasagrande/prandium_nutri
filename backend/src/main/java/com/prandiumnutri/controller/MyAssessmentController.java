package com.prandiumnutri.controller;

import com.prandiumnutri.domain.ClientAssessment;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.dto.AssessmentSelfFillDto;
import com.prandiumnutri.dto.AssessmentSnapshotDto;
import com.prandiumnutri.dto.ClientAssessmentDto;
import com.prandiumnutri.repository.ClientAssessmentRepository;
import com.prandiumnutri.repository.UserRepository;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.ClientAssessmentService;
import com.prandiumnutri.service.ConsultationService;
import com.prandiumnutri.service.PrescriptionPdfService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    private final ConsultationService consultationService;
    private final PrescriptionPdfService pdfService;
    private final com.prandiumnutri.service.PrescriptionIssuanceService issuanceService;
    private final ClientAssessmentRepository assessmentRepository;
    private final UserRepository userRepository;
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

    @GetMapping("/consultations")
    public List<com.prandiumnutri.dto.ConsultationDto> myConsultations() {
        return consultationService.listForClient(currentUser.getAuthenticatedId());
    }

    /** Fase 9D-3: cliente vê próprio log de emissões (todas as vezes que o PDF foi baixado). */
    @GetMapping("/issuances")
    public List<com.prandiumnutri.dto.PrescriptionIssuanceDto> myIssuances() {
        return issuanceService.listForClient(currentUser.getAuthenticatedId());
    }

    @GetMapping(value = "/prescription.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> myPrescriptionPdf() {
        Long clientId = currentUser.getAuthenticatedId();
        ClientAssessment a = assessmentRepository.findByClientIdOrderByUpdatedAtDesc(clientId).stream()
                .filter(x -> x.getTargetCalories() != null)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Voce ainda nao tem prescricao — aguarde seu nutricionista."));
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));

        byte[] pdf = pdfService.render(a, client, a.getNutritionist(),
                LocaleContextHolder.getLocale(), /* snapshot */ true, "CLIENT");
        String hash = com.prandiumnutri.service.PrescriptionIssuanceService.sha256Hex(pdf);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=minha-prescricao.pdf")
                .header("X-Pdf-Sha256", hash)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
