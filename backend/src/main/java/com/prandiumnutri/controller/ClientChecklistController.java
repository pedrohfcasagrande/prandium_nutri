package com.prandiumnutri.controller;

import com.prandiumnutri.dto.ClientChecklistDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.ClientChecklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/checklist")
@RequiredArgsConstructor
public class ClientChecklistController {

    private final ClientChecklistService service;
    private final CurrentUser currentUser;

    @GetMapping
    public ClientChecklistDto get() {
        return service.compute(currentUser.id());
    }
}
