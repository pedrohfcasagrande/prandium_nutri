package com.nutritracker.service;

import com.nutritracker.domain.PaymentProvider;
import com.nutritracker.domain.PlanTarget;
import com.nutritracker.domain.Subscription;
import com.nutritracker.domain.SubscriptionStatus;
import com.nutritracker.repository.PlanPriceRepository;
import com.nutritracker.repository.PlanRepository;
import com.nutritracker.support.PlanTestFixtures;
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
