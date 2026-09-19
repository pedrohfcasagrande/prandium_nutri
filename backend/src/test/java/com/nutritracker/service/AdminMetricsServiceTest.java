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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminMetricsServiceTest {

    @Autowired AdminMetricsService adminMetricsService;
    @Autowired SubscriptionService subscriptionService;
    @Autowired UserRepository userRepo;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void overview_returnsBaselineMetrics() {
        var overview = adminMetricsService.overview();
        assertThat(overview).isNotNull();
        assertThat(overview.totalUsers()).isGreaterThanOrEqualTo(0);
        assertThat(overview.mrrByCurrency()).isNotNull();
        assertThat(overview.planDistribution()).isNotNull();
        assertThat(overview.topCompanies()).isNotNull();
    }

    @Test
    void overview_reflectsNewSubscription() {
        User nutri = User.builder()
                .name("Metrics Nutri").email("metrics-" + System.nanoTime() + "@example.com")
                .passwordHash("$2a$10$xxx").role(Role.NUTRITIONIST).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .build();
        nutri = userRepo.save(nutri);
        subscriptionService.createFreeSubscription(PlanTarget.NUTRITIONIST, nutri.getId(), "BRL");

        var overview = adminMetricsService.overview();
        assertThat(overview.activeSubscriptions()).isGreaterThanOrEqualTo(1);
        assertThat(overview.planDistribution()).containsKey("NUTRI_FREE");
    }
}
