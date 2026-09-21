package com.prandiumnutri.controller;

import com.prandiumnutri.dto.ClientAssessmentDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.ClientAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cliente le a propria ficha (mais recente). Read-only. */
@RestController
@RequestMapping("/api/me/assessment")
@RequiredArgsConstructor
public class MyAssessmentController {

    private final ClientAssessmentService service;
    private final CurrentUser currentUser;

    @GetMapping
    public ResponseEntity<ClientAssessmentDto> get() {
        return service.findLatestForClient(currentUser.getAuthenticatedId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
