package com.prandiumnutri.controller;

import com.prandiumnutri.dto.AppointmentDto;
import com.prandiumnutri.dto.AppointmentRequestDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nutritionist/appointments")
@RequiredArgsConstructor
public class AppointmentNutriController {

    private final AppointmentService service;
    private final CurrentUser currentUser;

    @GetMapping
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public List<AppointmentDto> list(@RequestParam(required = false) Long practiceId) {
        return service.listForNutritionist(currentUser.getAuthenticatedId(), practiceId);
    }

    @PostMapping
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public AppointmentDto suggest(@Valid @RequestBody AppointmentRequestDto req) {
        return service.suggestByNutri(
                currentUser.getAuthenticated(),
                req.clientId(),
                req.practiceId(),
                req.scheduledAt(),
                req.durationMinutes(),
                req.location(),
                req.locationDetails(),
                req.notes());
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public AppointmentDto confirm(@PathVariable Long id) {
        return service.confirm(currentUser.getAuthenticated(), id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public AppointmentDto cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return service.cancel(currentUser.getAuthenticated(), id, reason);
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public AppointmentDto reschedule(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String iso = body.get("scheduledAt");
        return service.requestReschedule(currentUser.getAuthenticated(), id, LocalDateTime.parse(iso));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public AppointmentDto complete(@PathVariable Long id) {
        return service.markCompleted(currentUser.getAuthenticated(), id);
    }

    @PostMapping("/{id}/no-show")
    @PreAuthorize("hasRole('NUTRITIONIST')")
    public AppointmentDto noShow(@PathVariable Long id) {
        return service.markNoShow(currentUser.getAuthenticated(), id);
    }
}
