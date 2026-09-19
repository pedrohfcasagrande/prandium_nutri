package com.nutritracker.controller;

import com.nutritracker.dto.NutriMetricsDto;
import com.nutritracker.security.CurrentUser;
import com.nutritracker.service.NutriMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nutritionist/metrics")
@RequiredArgsConstructor
public class NutriMetricsController {

    private final NutriMetricsService metricsService;
    private final CurrentUser currentUser;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public NutriMetricsDto.Overview overview() {
        return metricsService.overview(currentUser.getAuthenticatedId());
    }
}
