package com.prandiumnutri.service;

import com.prandiumnutri.domain.PlanTarget;
import com.prandiumnutri.domain.Role;
import com.prandiumnutri.dto.RegisterRequest;
import com.prandiumnutri.repository.PlanPriceRepository;
import com.prandiumnutri.repository.PlanRepository;
import com.prandiumnutri.support.PlanTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired AuthService authService;
    @Autowired SubscriptionService subscriptionService;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void registerUser_createsUserAndFreeSubscription() {
        var req = new RegisterRequest("Test User", "test-user-" + System.nanoTime() + "@example.com",
                "senha1234", true, null);
        var resp = authService.register(req);

        assertThat(resp.role()).isEqualTo(Role.USER);
        assertThat(resp.profileCompleted()).isFalse();

        var sub = subscriptionService.effectiveOf(PlanTarget.USER, resp.userId());
        assertThat(sub).isPresent();
        assertThat(sub.get().getPlan().getCode()).isEqualTo("CLIENT_FREE");
    }

    @Test
    void registerNutritionist_createsNutriAndFreeSubscription() {
        var req = new RegisterRequest("Nutri Test", "nutri-" + System.nanoTime() + "@example.com",
                "senha1234", true, Role.NUTRITIONIST);
        var resp = authService.register(req);

        assertThat(resp.role()).isEqualTo(Role.NUTRITIONIST);
        assertThat(resp.profileCompleted()).isTrue();

        var sub = subscriptionService.effectiveOf(PlanTarget.NUTRITIONIST, resp.userId());
        assertThat(sub).isPresent();
        assertThat(sub.get().getPlan().getCode()).isEqualTo("NUTRI_FREE");
    }
}
