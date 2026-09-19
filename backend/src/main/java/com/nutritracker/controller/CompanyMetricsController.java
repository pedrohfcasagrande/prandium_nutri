package com.nutritracker.controller;

import com.nutritracker.dto.CompanyMetricsDto;
import com.nutritracker.security.CurrentUser;
import com.nutritracker.service.CompanyMetricsService;
import com.nutritracker.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company/metrics")
@RequiredArgsConstructor
public class CompanyMetricsController {

    private final CompanyMetricsService metricsService;
    private final CompanyService companyService;
    private final CurrentUser currentUser;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('COMPANY')")
    public CompanyMetricsDto.Overview overview() {
        Long companyId = companyService.getByOwner(currentUser.getAuthenticatedId()).getId();
        return metricsService.overview(companyId);
    }
}
