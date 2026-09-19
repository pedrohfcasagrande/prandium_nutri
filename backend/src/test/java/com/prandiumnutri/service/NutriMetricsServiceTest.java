package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.repository.*;
import com.prandiumnutri.support.PlanTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NutriMetricsServiceTest {

    @Autowired NutriMetricsService metricsService;
    @Autowired UserRepository userRepo;
    @Autowired NutritionistClientLinkRepository linkRepo;
    @Autowired MealRepository mealRepo;
    @Autowired FoodRepository foodRepo;
    @Autowired SubscriptionService subscriptionService;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void overview_noClients_returnsZeros() {
        User nutri = createNutri();
        var ov = metricsService.overview(nutri.getId());
        assertThat(ov.activeClients()).isZero();
        assertThat(ov.clients()).isEmpty();
        assertThat(ov.avgAdherence7d()).isZero();
    }

    @Test
    void overview_countsAdherenceFromMealsLast7Days() {
        User nutri = createNutri();
        User client = createClient();
        linkRepo.save(NutritionistClientLink.builder()
                .nutritionist(nutri).client(client)
                .invitedByEmail(client.getEmail())
                .inviteToken("t-" + System.nanoTime())
                .status(LinkStatus.ACTIVE)
                .invitedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build());

        Food banana = foodRepo.save(Food.builder()
                .name("Banana test").caloriesPer100g(89.0).proteinPer100g(1.1)
                .carbsPer100g(23.0).fatPer100g(0.3).build());

        // 3 refeicoes em 3 dias diferentes nos ultimos 7 dias -> adesao = 3/7
        for (int i = 0; i < 3; i++) {
            Meal m = new Meal();
            m.setUser(client);
            m.setDate(LocalDate.now().minusDays(i));
            m.setTime(LocalTime.NOON);
            m.setType(MealType.LUNCH);
            m.getItems().add(MealItem.builder().meal(m).food(banana).grams(100.0).build());
            mealRepo.save(m);
        }

        var ov = metricsService.overview(nutri.getId());
        assertThat(ov.activeClients()).isEqualTo(1);
        assertThat(ov.clients().get(0).adherence7d()).isEqualTo(0.43);  // Math.round(3/7 * 100)/100 = 0.43
        assertThat(ov.clients().get(0).needsAttention()).isFalse();     // 0.43 > 0.30
    }

    @Test
    void overview_flagsAttentionWhenAdherenceBelow30() {
        User nutri = createNutri();
        User client = createClient();
        linkRepo.save(NutritionistClientLink.builder()
                .nutritionist(nutri).client(client)
                .invitedByEmail(client.getEmail()).inviteToken("t-" + System.nanoTime())
                .status(LinkStatus.ACTIVE)
                .invitedAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(30))
                .build());

        // Cliente sem refeicoes -> adesao 0/7 -> needsAttention = true
        var ov = metricsService.overview(nutri.getId());
        assertThat(ov.clientsNeedingAttention()).isEqualTo(1);
        assertThat(ov.clients().get(0).needsAttention()).isTrue();
    }

    private User createNutri() {
        User u = userRepo.save(User.builder()
                .name("Nutri").email("n-" + System.nanoTime() + "@x.com")
                .role(Role.NUTRITIONIST).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").build());
        subscriptionService.createFreeSubscription(PlanTarget.NUTRITIONIST, u.getId(), "BRL");
        return u;
    }

    private User createClient() {
        return userRepo.save(User.builder()
                .name("Client").email("c-" + System.nanoTime() + "@x.com")
                .role(Role.USER).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").build());
    }
}
