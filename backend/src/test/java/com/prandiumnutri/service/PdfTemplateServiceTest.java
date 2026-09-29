package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.dto.PdfCustomizationDto;
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

/**
 * Fase 9H-1: resolveEffectiveFor decide corretamente entre template COMPANY vs PRACTICE.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PdfTemplateServiceTest {

    @Autowired PdfTemplateService service;
    @Autowired UserRepository userRepo;
    @Autowired NutritionistPracticeRepository practiceRepo;
    @Autowired CompanyRepository companyRepo;
    @Autowired SubscriptionService subscriptionService;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void resolveEffectiveFor_particular_returnsPracticeTemplate() {
        User nutri = createNutri();
        NutritionistPractice particular = practiceRepo.save(NutritionistPractice.builder()
                .nutritionist(nutri).company(null).acceptsNewClients(true).build());

        service.upsert(PdfTemplateOwnerType.PRACTICE, particular.getId(),
                dto("#111111", "Particular"));

        var resolved = service.resolveEffectiveFor(particular).orElseThrow();
        assertThat(resolved.getOwnerType()).isEqualTo(PdfTemplateOwnerType.PRACTICE);
        assertThat(resolved.getPdfHeaderLine()).isEqualTo("Particular");
    }

    @Test
    void resolveEffectiveFor_linkedPractice_returnsCompanyTemplate() {
        User nutri = createNutri();
        Company company = companyRepo.save(Company.builder()
                .name("Empresa X").taxId("11111111000191")
                .contactEmail("x@x.com")
                .owner(createOwner()).active(true).build());
        NutritionistPractice linked = practiceRepo.save(NutritionistPractice.builder()
                .nutritionist(nutri).company(company).acceptsNewClients(true).build());

        service.upsert(PdfTemplateOwnerType.COMPANY, company.getId(),
                dto("#222222", "Empresa X - Nutricao"));

        var resolved = service.resolveEffectiveFor(linked).orElseThrow();
        assertThat(resolved.getOwnerType()).isEqualTo(PdfTemplateOwnerType.COMPANY);
        assertThat(resolved.getPdfHeaderLine()).isEqualTo("Empresa X - Nutricao");
    }

    @Test
    void resolveEffectiveFor_linkedPractice_ignoresPracticeTemplate() {
        // Se por algum motivo houver template PRACTICE de uma practice vinculada
        // (impossivel via UI mas defensivo), resolveEffectiveFor NAO deve retorna-lo
        // — a regra e "vinculada = herda da empresa, sempre".
        User nutri = createNutri();
        Company company = companyRepo.save(Company.builder()
                .name("Empresa Y").taxId("22222222000192")
                .contactEmail("y@y.com")
                .owner(createOwner()).active(true).build());
        NutritionistPractice linked = practiceRepo.save(NutritionistPractice.builder()
                .nutritionist(nutri).company(company).acceptsNewClients(true).build());

        service.upsert(PdfTemplateOwnerType.PRACTICE, linked.getId(),
                dto("#333333", "Nao deveria aparecer"));

        // Empresa nao configurou template ainda
        assertThat(service.resolveEffectiveFor(linked)).isEmpty();
    }

    private PdfCustomizationDto dto(String color, String headerLine) {
        return new PdfCustomizationDto(null, color, headerLine,
                null, null, null, null, null, null);
    }

    private User createNutri() {
        User u = userRepo.save(User.builder()
                .name("Nutri").email("n-" + System.nanoTime() + "@x.com")
                .role(Role.NUTRITIONIST).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").cpf(cpf())
                .build());
        subscriptionService.createFreeSubscription(PlanTarget.NUTRITIONIST, u.getId(), "BRL");
        return u;
    }

    private User createOwner() {
        return userRepo.save(User.builder()
                .name("Owner").email("o-" + System.nanoTime() + "@x.com")
                .role(Role.COMPANY).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").cpf(cpf())
                .build());
    }

    private String cpf() { return String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)); }
}
