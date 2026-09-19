package com.prandiumnutri.service;

import com.prandiumnutri.domain.AuthProvider;
import com.prandiumnutri.domain.PlanTarget;
import com.prandiumnutri.domain.Role;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.repository.PlanPriceRepository;
import com.prandiumnutri.repository.PlanRepository;
import com.prandiumnutri.repository.UserRepository;
import com.prandiumnutri.support.PlanTestFixtures;
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
                .cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
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
    void listClients_exposesSubscriptionStatusOfLinkedClients() {
        var nutri = newNutri();
        User client = userRepo.save(User.builder()
                .name("Cliente Sub").email("csub-" + System.nanoTime() + "@example.com")
                .passwordHash("$2a$10$x").role(Role.USER).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now()).cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
                .build());
        subscriptionService.createFreeSubscription(PlanTarget.USER, client.getId(), "BRL");

        linkService.invite(nutri, client.getEmail());
        var list = linkService.listClientsForNutri(nutri.getId());

        assertThat(list).anySatisfy(l -> {
            assertThat(l.clientEmail()).isEqualTo(client.getEmail());
            assertThat(l.subscriptionStatus()).isNotNull();
            assertThat(l.subscriptionPlanCode()).isEqualTo("CLIENT_FREE");
        });
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
