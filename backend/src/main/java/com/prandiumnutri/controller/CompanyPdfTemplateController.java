package com.prandiumnutri.controller;

import com.prandiumnutri.domain.PdfTemplateOwnerType;
import com.prandiumnutri.dto.PdfCustomizationDto;
import com.prandiumnutri.dto.PdfTemplateView;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.CompanyService;
import com.prandiumnutri.service.PdfTemplateService;
import com.prandiumnutri.service.PrescriptionPdfService;
import com.prandiumnutri.domain.ClientAssessment;
import com.prandiumnutri.domain.NutritionistPractice;
import com.prandiumnutri.domain.Sex;
import com.prandiumnutri.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fase 9H-1: admin da empresa (role COMPANY) gerencia o template PDF que sera
 * usado por todas as practices vinculadas a essa empresa.
 */
@RestController
@RequestMapping("/api/company/pdf-template")
@RequiredArgsConstructor
public class CompanyPdfTemplateController {

    private final PdfTemplateService pdfTemplateService;
    private final CompanyService companyService;
    private final PrescriptionPdfService pdfService;
    private final CurrentUser currentUser;

    @GetMapping
    @PreAuthorize("hasRole('COMPANY')")
    public PdfTemplateView get() {
        Long companyId = companyService.getByOwner(currentUser.getAuthenticatedId()).getId();
        return new PdfTemplateView(
                pdfTemplateService.readOrEmpty(PdfTemplateOwnerType.COMPANY, companyId),
                false, null);
    }

    @PutMapping
    @PreAuthorize("hasRole('COMPANY')")
    public void put(@RequestBody PdfCustomizationDto dto) {
        Long companyId = companyService.getByOwner(currentUser.getAuthenticatedId()).getId();
        pdfTemplateService.upsert(PdfTemplateOwnerType.COMPANY, companyId, dto);
    }

    @GetMapping(value = "/preview", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<byte[]> preview() {
        // Preview com paciente ficticio. O nutri "fake" aqui e o proprio owner da empresa
        // so pra montar contexto valido — a customizacao vem do template da empresa via practice.company_id.
        User owner = currentUser.getAuthenticated();
        var company = companyService.getByOwner(owner.getId());

        User fakeClient = User.builder()
                .name("Maria Souza (preview)")
                .cpf("12345678901")
                .sex(Sex.FEMALE).birthDate(LocalDate.of(1990, 3, 15))
                .weightKg(65.0).heightCm(165.0)
                .build();

        // Practice fake vinculada a esta empresa — assim resolveEffectiveFor devolve template COMPANY.
        NutritionistPractice fakePractice = NutritionistPractice.builder()
                .company(company)
                .nutritionist(owner)
                .build();

        ClientAssessment fake = ClientAssessment.builder()
                .client(fakeClient).nutritionist(owner).practice(fakePractice)
                .assessmentDate(LocalDateTime.now())
                .weightKg(65.0).heightCm(165)
                .tmbFormula("MIFFLIN").activityFactor(1.55).calorieDeltaPct(-0.15)
                .proteinGPerKg(1.8).fatPct(0.28)
                .targetCalories(1720.0).targetProteinG(117.0).targetCarbsG(190.0)
                .targetFatG(53.5).targetWaterMl(2275.0)
                .prescribedAt(LocalDateTime.now())
                .notes("Beber agua ao longo do dia. Priorizar proteina magra e legumes.")
                .goalNotes("Reducao gradual de gordura corporal preservando massa magra.")
                .build();
        byte[] pdf = pdfService.render(fake, fakeClient, owner,
                LocaleContextHolder.getLocale(), /* snapshot */ false);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=preview.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
