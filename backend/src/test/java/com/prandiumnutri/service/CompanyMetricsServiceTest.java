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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CompanyMetricsServiceTest {

    @Autowired CompanyMetricsService metricsService;
    @Autowired CompanyService companyService;
    @Autowired CompanyRepository companyRepo;
    @Autowired CompanyNutritionistRepository cnRepo;
    @Autowired NutritionistClientLinkRepository linkRepo;
    @Autowired UserRepository userRepo;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;
    @Autowired NutritionistPracticeRepository practiceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void overview_emptyCompany_returnsZeros() {
        Company c = companyRepo.save(Company.builder()
                .name("Empty Co").contactEmail("empty@co.com").build());
        var ov = metricsService.overview(c.getId());

        assertThat(ov.nutritionistsActive()).isZero();
        assertThat(ov.nutritionistsPending()).isZero();
        assertThat(ov.clientsTotal()).isZero();
        assertThat(ov.nutritionists()).isEmpty();
    }

    @Test
    void overview_countsActiveNutrisAndClients() {
        Company c = companyRepo.save(Company.builder()
                .name("Co").contactEmail("co@x.com").build());
        User nutri = userRepo.save(User.builder()
                .name("Nutri A").email("nutri-" + System.nanoTime() + "@x.com")
                .role(Role.NUTRITIONIST).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
                .build());
        cnRepo.save(CompanyNutritionist.builder()
                .company(c).nutritionist(nutri)
                .invitedByEmail(nutri.getEmail()).inviteToken("t-" + System.nanoTime())
                .status(LinkStatus.ACTIVE)
                .invitedAt(LocalDateTime.now()).acceptedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build());
        NutritionistPractice practice = practiceRepo.save(NutritionistPractice.builder()
                .nutritionist(nutri).company(c)
                .acceptsNewClients(true).status(PracticeStatus.ACTIVE)
                .build());
        // 2 clientes ativos vinculados a este nutri via practice desta empresa
        for (int i = 0; i < 2; i++) {
            User client = userRepo.save(User.builder()
                    .name("Client " + i).email("client" + i + "-" + System.nanoTime() + "@x.com")
                    .role(Role.USER).provider(AuthProvider.LOCAL)
                    .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                    .passwordHash("$2a$10$x").cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
                .build());
            linkRepo.save(NutritionistClientLink.builder()
                    .nutritionist(nutri).client(client).practice(practice)
                    .invitedByEmail(client.getEmail())
                    .inviteToken("tc-" + System.nanoTime() + i)
                    .status(LinkStatus.ACTIVE)
                    .invitedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .build());
        }

        var ov = metricsService.overview(c.getId());
        assertThat(ov.nutritionistsActive()).isEqualTo(1);
        assertThat(ov.clientsTotal()).isEqualTo(2);
        assertThat(ov.nutritionists()).hasSize(1);
        assertThat(ov.nutritionists().get(0).activeClients()).isEqualTo(2);
    }
}
