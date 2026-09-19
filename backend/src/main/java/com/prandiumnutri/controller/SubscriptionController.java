package com.prandiumnutri.controller;

import com.prandiumnutri.domain.PlanTarget;
import com.prandiumnutri.domain.Role;
import com.prandiumnutri.domain.Subscription;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.dto.SubscriptionDto;
import com.prandiumnutri.repository.CompanyRepository;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.SubscriptionService;
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
