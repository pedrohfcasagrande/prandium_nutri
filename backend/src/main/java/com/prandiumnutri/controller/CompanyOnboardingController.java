package com.prandiumnutri.controller;

import com.prandiumnutri.dto.AcceptOnboardingRequest;
import com.prandiumnutri.dto.AuthResponse;
import com.prandiumnutri.dto.OnboardingInfoDto;
import com.prandiumnutri.service.CompanyOnboardingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/onboarding/company")
@RequiredArgsConstructor
public class CompanyOnboardingController {

    private final CompanyOnboardingService service;

    @GetMapping("/{token}")
    public OnboardingInfoDto lookup(@PathVariable String token) {
        return service.lookupToken(token);
    }

    @PostMapping("/{token}/accept")
    public AuthResponse accept(@PathVariable String token, @Valid @RequestBody AcceptOnboardingRequest req,
                               HttpServletRequest http) {
        return service.acceptOnboarding(token, req, http);
    }
}
