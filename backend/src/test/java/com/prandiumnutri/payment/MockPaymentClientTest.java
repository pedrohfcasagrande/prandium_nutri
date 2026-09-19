package com.prandiumnutri.payment;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.repository.PlanPriceRepository;
import com.prandiumnutri.repository.PlanRepository;
import com.prandiumnutri.repository.SubscriptionRepository;
import com.prandiumnutri.repository.UserRepository;
import com.prandiumnutri.service.SubscriptionService;
import com.prandiumnutri.support.PlanTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MockPaymentClientTest {

    @Autowired List<PaymentProviderClient> clients;
    @Autowired SubscriptionRepository subRepo;
    @Autowired SubscriptionService subscriptionService;
    @Autowired UserRepository userRepo;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void mockClientIsActiveInTestProfile() {
        // Em perfil test, MockPaymentClient e o unico Stripe/MP disponivel
        assertThat(clients).isNotEmpty();
        var stripeClient = clients.stream()
                .filter(c -> c.provider() == PaymentProvider.STRIPE)
                .findFirst().orElseThrow();
        assertThat(stripeClient).isInstanceOf(MockPaymentClient.class);
        assertThat(stripeClient.isConfigured()).isTrue();
    }

    @Test
    void mockCreateCheckout_activatesSubscriptionImmediately() {
        // Cria user
        User user = User.builder()
                .name("Test").email("mock-" + System.nanoTime() + "@example.com")
                .passwordHash("$2a$10$xxx")
                .role(Role.NUTRITIONIST).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true)
                .consentAcceptedAt(LocalDateTime.now())
                .build();
        user = userRepo.save(user);
        subscriptionService.createFreeSubscription(PlanTarget.NUTRITIONIST, user.getId(), "BRL");

        Plan plan = planRepo.findById("NUTRI_PRO").orElseThrow();
        PlanPrice price = priceRepo.findByPlanCodeAndCurrencyAndActiveTrue("NUTRI_PRO", "BRL").orElseThrow();

        var mock = clients.stream()
                .filter(c -> c instanceof MockPaymentClient)
                .findFirst().orElseThrow();

        var result = mock.createCheckout(user, plan, price,
                "http://localhost:4200/billing?checkout=success",
                "http://localhost:4200/pricing");

        assertThat(result.checkoutUrl()).contains("mock=true");
        assertThat(result.providerSessionId()).startsWith("mock_sub_");

        // Deve ter substituido a subscription FREE pela nova PRO
        var effective = subscriptionService.effectiveOf(PlanTarget.NUTRITIONIST, user.getId()).orElseThrow();
        assertThat(effective.getPlan().getCode()).isEqualTo("NUTRI_PRO");
        assertThat(effective.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }
}
