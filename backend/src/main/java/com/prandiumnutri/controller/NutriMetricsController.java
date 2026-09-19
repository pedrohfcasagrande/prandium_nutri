package com.prandiumnutri.controller;

import com.prandiumnutri.dto.NutriMetricsDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.NutriMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nutritionist/metrics")
@RequiredArgsConstructor
public class NutriMetricsController {

    private final NutriMetricsService metricsService;
    private final CurrentUser currentUser;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public NutriMetricsDto.Overview overview(@RequestParam(required = false) Long practiceId) {
        return metricsService.overview(currentUser.getAuthenticatedId(), practiceId);
    }
}
