package com.nutritracker.service;

import com.nutritracker.domain.AuthProvider;
import com.nutritracker.domain.PlanTarget;
import com.nutritracker.domain.Role;
import com.nutritracker.domain.User;
import com.nutritracker.repository.PlanPriceRepository;
import com.nutritracker.repository.PlanRepository;
import com.nutritracker.repository.UserRepository;
import com.nutritracker.support.PlanTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NutritionistLinkServiceTest {

    @Autowired NutritionistLinkService linkService;
    @Autowired SubscriptionService subscriptionService;
    @Autowired UserRepository userRepo;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    private User newNutri() {
        User u = User.builder()
                .name("Nutri Test")
                .email("nutri-" + System.nanoTime() + "@example.com")
                .passwordHash("$2a$10$abcdefghijklmnopqrstuv")
                .role(Role.NUTRITIONIST)
                .provider(AuthProvider.LOCAL)
                .profileCompleted(true)
                .active(true)
                .consentAcceptedAt(LocalDateTime.now())
                .build();
        u = userRepo.save(u);
        subscriptionService.createFreeSubscription(PlanTarget.NUTRITIONIST, u.getId(), "BRL");
        return u;
    }

    @Test
    void invite_succeedsWithinLimit() {
        var nutri = newNutri();
        var link = linkService.invite(nutri, "cliente1@example.com");
        assertThat(link).isNotNull();
        assertThat(link.clientEmail()).isEqualTo("cliente1@example.com");
    }

    @Test
    void invite_blocksWhenFreePlanLimitReached() {
        var nutri = newNutri();
        // NUTRI_FREE: maxClients = 3
        linkService.invite(nutri, "c1@example.com");
        linkService.invite(nutri, "c2@example.com");
        linkService.invite(nutri, "c3@example.com");

        assertThatThrownBy(() -> linkService.invite(nutri, "c4@example.com"))
                .isInstanceOf(PlanLimitExceededException.class)
                .hasMessageContaining("maxClients");
    }
}
