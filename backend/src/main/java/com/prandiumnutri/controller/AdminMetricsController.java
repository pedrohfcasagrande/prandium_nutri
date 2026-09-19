package com.prandiumnutri.controller;

import com.prandiumnutri.dto.AdminMetricsDto;
import com.prandiumnutri.service.AdminMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/metrics")
@RequiredArgsConstructor
public class AdminMetricsController {

    private final AdminMetricsService service;

    @GetMapping("/overview")
    public AdminMetricsDto.Overview overview() {
        return service.overview();
    }
}
