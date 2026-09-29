package com.prandiumnutri.controller;

import com.prandiumnutri.dto.AppointmentDto;
import com.prandiumnutri.dto.AppointmentRequestDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentClientController {

    private final AppointmentService service;
    private final CurrentUser currentUser;

    @GetMapping
    public List<AppointmentDto> list() {
        return service.listForClient(currentUser.getAuthenticatedId());
    }

    @PostMapping
    public AppointmentDto request(@Valid @RequestBody AppointmentRequestDto req) {
        return service.requestByClient(
                currentUser.getAuthenticated(),
                req.nutritionistId(),
                req.scheduledAt(),
                req.durationMinutes(),
                req.location(),
                req.locationDetails(),
                req.meetingUrl(),
                req.notes());
    }

    @PostMapping("/{id}/confirm")
    public AppointmentDto confirm(@PathVariable Long id) {
        return service.confirm(currentUser.getAuthenticated(), id);
    }

    @PostMapping("/{id}/cancel")
    public AppointmentDto cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return service.cancel(currentUser.getAuthenticated(), id, reason);
    }

    @PostMapping("/{id}/reschedule")
    public AppointmentDto reschedule(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String iso = body.get("scheduledAt");
        return service.requestReschedule(currentUser.getAuthenticated(), id, LocalDateTime.parse(iso));
    }
}
