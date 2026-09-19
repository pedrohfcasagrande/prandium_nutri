package com.prandiumnutri.controller;

import com.prandiumnutri.dto.DailySummaryDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUser currentUser;

    @GetMapping
    public DailySummaryDto summary(@RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dashboardService.summary(currentUser.id(), date == null ? LocalDate.now() : date);
    }
}
