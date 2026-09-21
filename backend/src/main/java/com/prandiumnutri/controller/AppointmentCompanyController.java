package com.prandiumnutri.controller;

import com.prandiumnutri.dto.AppointmentDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.AppointmentService;
import com.prandiumnutri.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company/appointments")
@RequiredArgsConstructor
public class AppointmentCompanyController {

    private final AppointmentService service;
    private final CompanyService companyService;
    private final CurrentUser currentUser;

    @GetMapping
    @PreAuthorize("hasRole('COMPANY')")
    public List<AppointmentDto> list() {
        Long companyId = companyService.getByOwner(currentUser.getAuthenticatedId()).getId();
        return service.listForCompany(companyId);
    }
}
