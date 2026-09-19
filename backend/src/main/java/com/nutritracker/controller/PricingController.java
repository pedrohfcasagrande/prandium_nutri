package com.nutritracker.controller;

import com.nutritracker.domain.PlanTarget;
import com.nutritracker.dto.PricingDto;
import com.nutritracker.service.CurrencyDetectionService;
import com.nutritracker.service.PricingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;
    private final CurrencyDetectionService currencyService;

    /**
     * Endpoint publico (via SecurityConfig permit /api/pricing/**).
     * currency opcional: se ausente, detecta pelo IP/idioma.
     */
    @GetMapping
    public PricingDto pricing(
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) PlanTarget target,
            HttpServletRequest request) {
        String cur = (currency != null && currencyService.isSupported(currency))
                ? currency.toUpperCase()
                : currencyService.detectCurrency(clientIp(request), request.getHeader("Accept-Language"));
        return pricingService.pricing(cur, target);
    }

    @GetMapping("/detect-currency")
    public java.util.Map<String, String> detect(HttpServletRequest request) {
        String cur = currencyService.detectCurrency(clientIp(request), request.getHeader("Accept-Language"));
        return java.util.Map.of("currency", cur);
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
