package com.nutritracker.service;

import com.nutritracker.domain.PlanTarget;
import com.nutritracker.domain.Subscription;
import com.nutritracker.repository.PlanPriceRepository;
import com.nutritracker.repository.PlanRepository;
import com.nutritracker.support.PlanTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FeatureGateServiceTest {

    @Autowired FeatureGateService featureGate;
    @Autowired SubscriptionService subscriptionService;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void assertCanUseAiSuggestion_freeUser_allowsUpToLimit() {
        long userId = 500L;
        subscriptionService.createFreeSubscription(PlanTarget.USER, userId, "BRL");

        // CLIENT_FREE tem aiSuggestionsMonthly=3
        assertThatCode(() -> featureGate.assertCanUseAiSuggestion(PlanTarget.USER, userId))
                .doesNotThrowAnyException();
        featureGate.recordAiSuggestionUsage(PlanTarget.USER, userId);
        featureGate.recordAiSuggestionUsage(PlanTarget.USER, userId);
        featureGate.recordAiSuggestionUsage(PlanTarget.USER, userId);

        // 4a chamada deve estourar
        assertThatThrownBy(() -> featureGate.assertCanUseAiSuggestion(PlanTarget.USER, userId))
                .isInstanceOf(PlanLimitExceededException.class)
                .hasMessageContaining("aiSuggestionsMonthly");
    }

    @Test
    void assertCanUseAiSuggestion_unlimitedPlan_neverThrows() {
        long userId = 501L;
        Subscription free = subscriptionService.createFreeSubscription(PlanTarget.USER, userId, "BRL");
        // Simula upgrade para CLIENT_PLUS (unlimited)
        var plusPlan = planRepo.findById("CLIENT_PLUS").orElseThrow();
        free.setPlan(plusPlan);
        subscriptionService.getClass(); // no-op

        // Faz 20 chamadas - nao lanca
        for (int i = 0; i < 20; i++) {
            featureGate.recordAiSuggestionUsage(PlanTarget.USER, userId);
        }
        assertThatCode(() -> featureGate.assertCanUseAiSuggestion(PlanTarget.USER, userId))
                .doesNotThrowAnyException();
    }
}
