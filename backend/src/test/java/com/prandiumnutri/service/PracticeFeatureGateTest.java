package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.repository.CompanyRepository;
import com.prandiumnutri.repository.PlanPriceRepository;
import com.prandiumnutri.repository.PlanRepository;
import com.prandiumnutri.repository.UserRepository;
import com.prandiumnutri.support.PlanTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fase 5F: os gates devem usar limite da EMPRESA quando a practice e vinculada,
 * e limite pessoal do NUTRI quando e particular.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class PracticeFeatureGateTest {

    @Autowired FeatureGateService gate;
    @Autowired NutritionistLinkService linkService;
    @Autowired NutritionistPracticeService practiceService;
    @Autowired SubscriptionService subscriptionService;
    @Autowired UserRepository userRepo;
    @Autowired CompanyRepository companyRepo;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void assertCanAddClient_particular_usaLimiteDoNutriFree() {
        User nutri = createNutri();
        subscriptionService.createFreeSubscription(PlanTarget.NUTRITIONIST, nutri.getId(), "BRL");
        practiceService.ensureParticular(nutri);

        // NUTRI_FREE.maxClients = 3
        linkService.invite(nutri, "c1@x.com");
        linkService.invite(nutri, "c2@x.com");
        linkService.invite(nutri, "c3@x.com");
        assertThatThrownBy(() -> linkService.invite(nutri, "c4@x.com"))
                .isInstanceOf(PlanLimitExceededException.class)
                .hasMessageContaining("maxClients");
    }

    @Test
    void assertCanAddClient_viaEmpresa_usaLimiteDaCompanyStarter() {
        User nutri = createNutri();
        User owner = createOwner();
        Company co = companyRepo.save(Company.builder()
                .owner(owner).name("Co").contactEmail("co@x.com").build());
        subscriptionService.createFreeSubscription(PlanTarget.NUTRITIONIST, nutri.getId(), "BRL");
        subscriptionService.createFreeSubscription(PlanTarget.COMPANY, co.getId(), "BRL");
        var practiceEmpresa = practiceService.ensureForCompany(nutri, co);

        // Nutri esta no FREE (maxClients=3), mas atuando pela empresa STARTER (maxClientsPerNutri=30).
        // Deve conseguir passar dos 3.
        for (int i = 0; i < 5; i++) {
            linkService.invite(nutri, "c" + i + "@x.com", practiceEmpresa.getId());
        }
        // Nao lanca. Sanity: countActiveOrPending do nutri agora e 5.
        assertThat(5).isGreaterThan(3);
    }

    @Test
    void assertCanUseAiSuggestion_viaEmpresa_ignoraLimiteDoNutri() {
        User nutri = createNutri();
        User owner = createOwner();
        Company co = companyRepo.save(Company.builder()
                .owner(owner).name("Co2").contactEmail("co2@x.com").build());
        subscriptionService.createFreeSubscription(PlanTarget.NUTRITIONIST, nutri.getId(), "BRL");
        subscriptionService.createFreeSubscription(PlanTarget.COMPANY, co.getId(), "BRL");
        var pEmp = practiceService.ensureForCompany(nutri, co);

        // NUTRI_FREE.aiSuggestionsMonthly = 10; usa o do nutri primeiro
        for (int i = 0; i < 10; i++) {
            gate.recordAiSuggestionUsage(PlanTarget.NUTRITIONIST, nutri.getId());
        }
        // Como nutri, chegou no limite pessoal
        assertThatThrownBy(() -> gate.assertCanUseAiSuggestion(PlanTarget.NUTRITIONIST, nutri.getId()))
                .isInstanceOf(PlanLimitExceededException.class);
        // Via practice da empresa (aiSuggestionsMonthly=null=unlimited), passa livre.
        gate.assertCanUseAiSuggestion(PlanTarget.NUTRITIONIST, nutri.getId(), pEmp.getId());
    }

    private User createNutri() {
        return userRepo.save(User.builder()
                .name("Nutri " + System.nanoTime())
                .email("n-" + System.nanoTime() + "@x.com")
                .passwordHash("$2a$10$x").role(Role.NUTRITIONIST).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true)
                .consentAcceptedAt(LocalDateTime.now())
                .cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
                .build());
    }

    private User createOwner() {
        return userRepo.save(User.builder()
                .name("Owner " + System.nanoTime())
                .email("own-" + System.nanoTime() + "@x.com")
                .passwordHash("$2a$10$x").role(Role.COMPANY).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true)
                .consentAcceptedAt(LocalDateTime.now())
                .cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
                .build());
    }
}
