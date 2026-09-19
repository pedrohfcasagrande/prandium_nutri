package com.prandiumnutri.controller;

import com.prandiumnutri.domain.SuggestionScope;
import com.prandiumnutri.dto.SuggestionResponse;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.MenuSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/suggestions")
@RequiredArgsConstructor
public class SuggestionController {

    private final MenuSuggestionService service;
    private final CurrentUser currentUser;

    @PostMapping
    public SuggestionResponse suggest(@RequestBody(required = false) Map<String, String> body) {
        String notes = body == null ? null : body.get("extraNotes");
        SuggestionScope scope = SuggestionScope.DAY;
        if (body != null && body.get("scope") != null) {
            try {
                scope = SuggestionScope.valueOf(body.get("scope").toUpperCase());
            } catch (IllegalArgumentException ignored) { }
        }
        return service.suggest(currentUser.id(), notes, scope);
    }
}
