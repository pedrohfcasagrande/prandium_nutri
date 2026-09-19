package com.nutritracker.controller;

import com.nutritracker.domain.PlanTarget;
import com.nutritracker.domain.Role;
import com.nutritracker.domain.Subscription;
import com.nutritracker.domain.User;
import com.nutritracker.dto.SubscriptionDto;
import com.nutritracker.repository.CompanyRepository;
import com.nutritracker.security.CurrentUser;
import com.nutritracker.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CurrentUser currentUser;
    private final CompanyRepository companyRepository;

    /** Retorna a subscription "efetiva" do usuario logado. */
    @GetMapping("/me")
    public ResponseEntity<SubscriptionDto> me() {
        User u = currentUser.getAuthenticated();
        var pair = resolvePayer(u);
        if (pair == null) return ResponseEntity.noContent().build();
        Subscription sub = subscriptionService.ensureSubscription(pair.type(), pair.id(), "BRL");
        return ResponseEntity.ok(subscriptionService.toDto(sub));
    }

    private Payer resolvePayer(User u) {
        return switch (u.getRole()) {
            case USER -> new Payer(PlanTarget.USER, u.getId());
            case NUTRITIONIST -> new Payer(PlanTarget.NUTRITIONIST, u.getId());
            case COMPANY -> {
                Long companyId = companyRepository.findByOwnerId(u.getId())
                        .map(c -> c.getId())
                        .orElse(null);
                yield companyId != null ? new Payer(PlanTarget.COMPANY, companyId) : null;
            }
            case ADMIN -> null;
        };
    }

    private record Payer(PlanTarget type, Long id) {}
}
