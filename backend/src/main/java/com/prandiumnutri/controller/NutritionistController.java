package com.prandiumnutri.controller;

import com.prandiumnutri.domain.ClientAssessment;
import com.prandiumnutri.domain.NutritionistPractice;
import com.prandiumnutri.domain.PdfTemplateOwnerType;
import com.prandiumnutri.domain.Sex;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.dto.ClientLinkDto;
import com.prandiumnutri.dto.InviteClientRequest;
import com.prandiumnutri.dto.PdfCustomizationDto;
import com.prandiumnutri.dto.PdfTemplateView;
import com.prandiumnutri.repository.UserRepository;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.NutritionistLinkService;
import com.prandiumnutri.service.NutritionistPracticeService;
import com.prandiumnutri.service.PdfTemplateService;
import com.prandiumnutri.service.PrescriptionPdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nutritionist")
@RequiredArgsConstructor
public class NutritionistController {

    private final NutritionistLinkService linkService;
    private final NutritionistPracticeService practiceService;
    private final PdfTemplateService pdfTemplateService;
    private final UserRepository userRepository;
    private final PrescriptionPdfService pdfService;
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

    // Fase 9H-1: PDF template com resolucao COMPANY vs PRACTICE.
    // Practice vinculada -> le template da empresa (readOnly=true).
    // Practice particular -> le/edita template proprio.

    @GetMapping("/practices/{practiceId}/pdf-template")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public PdfTemplateView getPdfTemplate(@PathVariable Long practiceId) {
        NutritionistPractice p = practiceService.getOwnedByNutri(
                currentUser.getAuthenticatedId(), practiceId);
        if (p.getCompany() != null) {
            return new PdfTemplateView(
                    pdfTemplateService.readOrEmpty(PdfTemplateOwnerType.COMPANY, p.getCompany().getId()),
                    true,
                    p.getCompany().getName());
        }
        return new PdfTemplateView(
                pdfTemplateService.readOrEmpty(PdfTemplateOwnerType.PRACTICE, p.getId()),
                false, null);
    }

    @PutMapping("/practices/{practiceId}/pdf-template")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public void putPdfTemplate(@PathVariable Long practiceId,
                                @RequestBody PdfCustomizationDto dto) {
        NutritionistPractice p = practiceService.getOwnedByNutri(
                currentUser.getAuthenticatedId(), practiceId);
        if (p.getCompany() != null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Practice vinculada a empresa — edicao via /api/company/pdf-template pelo admin");
        }
        pdfTemplateService.upsert(PdfTemplateOwnerType.PRACTICE, p.getId(), dto);
    }

    /** Preview PDF: renderiza com a customizacao da practice indicada; nao gera snapshot. */
    @GetMapping(value = "/practices/{practiceId}/pdf-preview", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public ResponseEntity<byte[]> previewPdf(@PathVariable Long practiceId) {
        User nutri = currentUser.getAuthenticated();
        NutritionistPractice p = practiceService.getOwnedByNutri(nutri.getId(), practiceId);
        User fakeClient = User.builder()
                .name("Maria Souza (preview)")
                .cpf("12345678901")
                .sex(Sex.FEMALE).birthDate(LocalDate.of(1990, 3, 15))
                .weightKg(65.0).heightCm(165.0)
                .build();
        ClientAssessment fake = ClientAssessment.builder()
                .client(fakeClient).nutritionist(nutri).practice(p)
                .assessmentDate(LocalDateTime.now())
                .weightKg(65.0).heightCm(165)
                .tmbFormula("MIFFLIN")
                .activityFactor(1.55)
                .calorieDeltaPct(-0.15)
                .proteinGPerKg(1.8)
                .fatPct(0.28)
                .targetCalories(1720.0)
                .targetProteinG(117.0)
                .targetCarbsG(190.0)
                .targetFatG(53.5)
                .targetWaterMl(2275.0)
                .prescribedAt(LocalDateTime.now())
                .notes("Beber agua ao longo do dia. Priorizar proteina magra e legumes.")
                .goalNotes("Reducao gradual de gordura corporal preservando massa magra.")
                .build();
        byte[] pdf = pdfService.render(fake, fakeClient, nutri,
                LocaleContextHolder.getLocale(), /* snapshot */ false);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=preview.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
