package com.prandiumnutri.controller;

import com.prandiumnutri.domain.PdfChangeRequestStatus;
import com.prandiumnutri.dto.PdfChangeRequestDto;
import com.prandiumnutri.dto.PdfCustomizationDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.CompanyService;
import com.prandiumnutri.service.PdfTemplateChangeRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Fase 9H-3: endpoints do fluxo de aprovacao.
 *
 * <ul>
 *   <li>Nutri vinculado: POST /api/nutritionist/pdf-change-requests + GET (proprias PENDING)</li>
 *   <li>Admin da empresa: GET /api/company/pdf-change-requests + POST /{id}/approve|reject</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class PdfChangeRequestController {

    private final PdfTemplateChangeRequestService service;
    private final CompanyService companyService;
    private final CurrentUser currentUser;

    // -------------------- Nutri --------------------

    @PostMapping("/api/nutritionist/pdf-change-requests")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public PdfChangeRequestDto create(@RequestBody CreateRequestBody body) {
        return service.create(currentUser.getAuthenticated(), body.practiceId(),
                body.proposed(), body.note());
    }

    @GetMapping("/api/nutritionist/pdf-change-requests/mine-pending")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public List<PdfChangeRequestDto> myPending() {
        return service.listOwnPending(currentUser.getAuthenticatedId());
    }

    // -------------------- Admin da empresa --------------------

    @GetMapping("/api/company/pdf-change-requests")
    @PreAuthorize("hasRole('COMPANY')")
    public List<PdfChangeRequestDto> listForCompany(
            @RequestParam(required = false) PdfChangeRequestStatus status) {
        Long companyId = companyService.getByOwner(currentUser.getAuthenticatedId()).getId();
        return service.listForCompany(companyId, status);
    }

    @PostMapping("/api/company/pdf-change-requests/{id}/approve")
    @PreAuthorize("hasRole('COMPANY')")
    public PdfChangeRequestDto approve(@PathVariable Long id,
                                        @RequestBody(required = false) Map<String, String> body) {
        String note = body == null ? null : body.get("note");
        return service.approve(currentUser.getAuthenticated(), id, note);
    }

    @PostMapping("/api/company/pdf-change-requests/{id}/reject")
    @PreAuthorize("hasRole('COMPANY')")
    public PdfChangeRequestDto reject(@PathVariable Long id,
                                       @RequestBody(required = false) Map<String, String> body) {
        String note = body == null ? null : body.get("note");
        return service.reject(currentUser.getAuthenticated(), id, note);
    }

    public record CreateRequestBody(
            Long practiceId,
            PdfCustomizationDto proposed,
            String note
    ) {}
}
