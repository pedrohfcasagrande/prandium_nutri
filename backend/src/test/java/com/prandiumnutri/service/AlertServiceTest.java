package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.repository.AlertRepository;
import com.prandiumnutri.repository.MealRepository;
import com.prandiumnutri.repository.NutritionistClientLinkRepository;
import com.prandiumnutri.repository.PlanPriceRepository;
import com.prandiumnutri.repository.PlanRepository;
import com.prandiumnutri.repository.SubscriptionRepository;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AlertServiceTest {

    @Autowired AlertService alertService;
    @Autowired AlertRepository alertRepository;
    @Autowired SubscriptionRepository subRepo;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;
    @Autowired UserRepository userRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void evaluatePaymentPastDue_createsAdminAlert() {
        Plan plan = planRepo.findById("NUTRI_PRO").orElseThrow();
        Subscription s = Subscription.builder()
                .payerType(PlanTarget.NUTRITIONIST).payerId(999L)
                .plan(plan).currency("BRL").amountCents(7900)
                .provider(PaymentProvider.MERCADO_PAGO)
                .status(SubscriptionStatus.PAST_DUE)
                .currentPeriodStart(LocalDateTime.now().minusMonths(2))
                .currentPeriodEnd(LocalDateTime.now().minusDays(15))
                .build();
        subRepo.save(s);

        int created = alertService.evaluatePaymentPastDue();
        assertThat(created).isGreaterThanOrEqualTo(1);

        var alerts = alertService.listActive(AlertScope.ADMIN, null);
        assertThat(alerts).anyMatch(a -> a.getKind().equals("PAYMENT_PAST_DUE"));
    }

    @Test
    void dismiss_marksAlertAsDismissed() {
        Alert a = alertRepository.save(Alert.builder()
                .scope(AlertScope.ADMIN).severity(AlertSeverity.INFO)
                .kind("TEST").title("test").build());

        alertService.dismiss(a.getId(), 1L);
        assertThat(alertRepository.findById(a.getId()).orElseThrow().getDismissedAt()).isNotNull();
    }
}
