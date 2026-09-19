package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.repository.NutritionistClientLinkRepository;
import com.prandiumnutri.repository.PlanPriceRepository;
import com.prandiumnutri.repository.PlanRepository;
import com.prandiumnutri.repository.UserRepository;
import com.prandiumnutri.support.PlanTestFixtures;
import java.time.LocalDateTime;
import java.util.UUID;
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
class SubscriptionServiceTest {

    @Autowired SubscriptionService subscriptionService;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;
    @Autowired UserRepository userRepo;
    @Autowired NutritionistClientLinkRepository nclRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) {
            PlanTestFixtures.seedAll(planRepo, priceRepo);
        }
    }

    @Test
    void createsFreeSubscriptionForNutritionist() {
        Subscription sub = subscriptionService.createFreeSubscription(
                PlanTarget.NUTRITIONIST, 100L, "BRL");

        assertThat(sub).isNotNull();
        assertThat(sub.getPlan().getCode()).isEqualTo("NUTRI_FREE");
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.getProvider()).isEqualTo(PaymentProvider.MANUAL);
        assertThat(sub.getAmountCents()).isZero();
        assertThat(sub.getCurrency()).isEqualTo("BRL");
    }

    @Test
    void createsFreeSubscriptionForUser() {
        Subscription sub = subscriptionService.createFreeSubscription(
                PlanTarget.USER, 200L, "USD");
        assertThat(sub.getPlan().getCode()).isEqualTo("CLIENT_FREE");
        assertThat(sub.getCurrency()).isEqualTo("USD");
    }

    @Test
    void effectiveOfReturnsEmptyWhenNoSubscription() {
        var res = subscriptionService.effectiveOf(PlanTarget.NUTRITIONIST, 999L);
        assertThat(res).isEmpty();
    }

    @Test
    void ensureSubscriptionCreatesFreeIfMissing() {
        var sub = subscriptionService.ensureSubscription(PlanTarget.USER, 300L, "EUR");
        assertThat(sub.getPlan().getCode()).isEqualTo("CLIENT_FREE");
        assertThat(sub.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void toDto_populatesMaxClientsHeadcountForNutritionist() {
        User nutri = userRepo.save(User.builder()
                .name("Nutri Head").email("nh-" + System.nanoTime() + "@x.com")
                .passwordHash("$2a$10$x").role(Role.NUTRITIONIST).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now()).build());
        var sub = subscriptionService.createFreeSubscription(PlanTarget.NUTRITIONIST, nutri.getId(), "BRL");

        // Cria 4 vinculos ACTIVE — acima do limite NUTRI_FREE (3)
        for (int i = 0; i < 4; i++) {
            User client = userRepo.save(User.builder()
                    .name("C" + i).email("c" + i + "-" + System.nanoTime() + "@x.com")
                    .passwordHash("$2a$10$x").role(Role.USER).provider(AuthProvider.LOCAL)
                    .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now()).build());
            nclRepo.save(NutritionistClientLink.builder()
                    .nutritionist(nutri).client(client)
                    .invitedByEmail(client.getEmail()).inviteToken(UUID.randomUUID().toString())
                    .status(LinkStatus.ACTIVE).invitedAt(LocalDateTime.now())
                    .acceptedAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusYears(1))
                    .initiatedByClient(false).build());
        }

        var dto = subscriptionService.toDto(sub);
        assertThat(dto.currentMonthUsage()).containsEntry("maxClients", 4);
    }

    @Test
    void incrementUsageAccumulates() {
        var sub = subscriptionService.createFreeSubscription(
                PlanTarget.USER, 400L, "BRL");
        subscriptionService.incrementUsage(sub.getId(), "aiSuggestionsMonthly");
        subscriptionService.incrementUsage(sub.getId(), "aiSuggestionsMonthly");
        subscriptionService.incrementUsage(sub.getId(), "aiSuggestionsMonthly");

        int used = subscriptionService.getCurrentMonthUsage(sub.getId(), "aiSuggestionsMonthly");
        assertThat(used).isEqualTo(3);
    }
}
