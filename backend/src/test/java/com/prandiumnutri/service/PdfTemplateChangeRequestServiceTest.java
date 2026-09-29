package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.dto.PdfChangeRequestDto;
import com.prandiumnutri.dto.PdfCustomizationDto;
import com.prandiumnutri.dto.PdfTemplateFieldDto;
import com.prandiumnutri.repository.*;
import com.prandiumnutri.support.PlanTestFixtures;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fase 9H-3: fluxo completo — nutri sugere -> admin aprova -> template atualizado + nutri notificado.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PdfTemplateChangeRequestServiceTest {

    @Autowired PdfTemplateChangeRequestService service;
    @Autowired PdfTemplateService templateService;
    @Autowired UserRepository userRepo;
    @Autowired NutritionistPracticeRepository practiceRepo;
    @Autowired CompanyRepository companyRepo;
    @Autowired NotificationRepository notificationRepo;
    @Autowired EntityManager em;
    @Autowired SubscriptionService subscriptionService;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void createApprove_updatesTemplateAndNotifies() {
        User owner = createOwner();
        Company company = companyRepo.save(Company.builder()
                .name("Empresa").taxId("12345678000199").contactEmail("c@x.com")
                .owner(owner).active(true).build());
        User nutri = createNutri();
        NutritionistPractice linked = practiceRepo.save(NutritionistPractice.builder()
                .nutritionist(nutri).company(company).acceptsNewClients(true).build());

        // Nutri sugere um template novo
        PdfCustomizationDto proposed = new PdfCustomizationDto(null, "#abcdef", "Nova sede",
                List.of(new PdfTemplateFieldDto(null, "Tel", PdfFieldType.TEL, "1234")));
        PdfChangeRequestDto req = service.create(nutri, linked.getId(), proposed, "Adicionar contato oficial");

        assertThat(req.status()).isEqualTo(PdfChangeRequestStatus.PENDING);
        // Owner recebeu notificacao
        assertThat(notificationRepo.findByUserIdOrderByCreatedAtDesc(
                owner.getId(), org.springframework.data.domain.PageRequest.of(0, 10)))
                .anyMatch(n -> n.getTitle().contains("sugestao"));

        // Admin aprova
        PdfChangeRequestDto after = service.approve(owner, req.id(), null);
        assertThat(after.status()).isEqualTo(PdfChangeRequestStatus.APPROVED);

        // Template da empresa foi atualizado com o valor proposto
        em.flush(); em.clear();
        PdfTemplate t = templateService.resolveEffectiveFor(linked).orElseThrow();
        assertThat(t.getPdfHeaderLine()).isEqualTo("Nova sede");
        assertThat(t.getPdfPrimaryColor()).isEqualTo("#abcdef");
        List<PdfTemplateField> fields = templateService.listFields(t.getId());
        assertThat(fields).hasSize(1);
        assertThat(fields.get(0).getLabel()).isEqualTo("Tel");

        // Nutri recebeu notificacao de aprovacao
        assertThat(notificationRepo.findByUserIdOrderByCreatedAtDesc(
                nutri.getId(), org.springframework.data.domain.PageRequest.of(0, 10)))
                .anyMatch(n -> n.getTitle().contains("aprovada"));
    }

    @Test
    void reject_savesReasonAndNotifies() {
        User owner = createOwner();
        Company company = companyRepo.save(Company.builder()
                .name("E2").taxId("22222222000199").contactEmail("y@x.com")
                .owner(owner).active(true).build());
        User nutri = createNutri();
        NutritionistPractice linked = practiceRepo.save(NutritionistPractice.builder()
                .nutritionist(nutri).company(company).acceptsNewClients(true).build());

        PdfChangeRequestDto req = service.create(nutri, linked.getId(),
                new PdfCustomizationDto(null, null, "H", List.of()), "por favor");
        PdfChangeRequestDto after = service.reject(owner, req.id(), "Layout nao segue a marca");
        assertThat(after.status()).isEqualTo(PdfChangeRequestStatus.REJECTED);
        assertThat(after.reviewNote()).isEqualTo("Layout nao segue a marca");
        assertThat(notificationRepo.findByUserIdOrderByCreatedAtDesc(
                nutri.getId(), org.springframework.data.domain.PageRequest.of(0, 10)))
                .anyMatch(n -> n.getTitle().contains("rejeitada"));
    }

    @Test
    void approve_deniedWhenReviewerIsNotAdmin() {
        User owner = createOwner();
        Company company = companyRepo.save(Company.builder()
                .name("E3").taxId("33333333000199").contactEmail("z@x.com")
                .owner(owner).active(true).build());
        User nutri = createNutri();
        NutritionistPractice linked = practiceRepo.save(NutritionistPractice.builder()
                .nutritionist(nutri).company(company).acceptsNewClients(true).build());
        User outsider = createOwner();

        PdfChangeRequestDto req = service.create(nutri, linked.getId(),
                new PdfCustomizationDto(null, null, "x", List.of()), null);
        assertThatThrownBy(() -> service.approve(outsider, req.id(), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void create_deniedForParticularPractice() {
        User nutri = createNutri();
        NutritionistPractice particular = practiceRepo.save(NutritionistPractice.builder()
                .nutritionist(nutri).company(null).acceptsNewClients(true).build());
        assertThatThrownBy(() -> service.create(nutri, particular.getId(),
                new PdfCustomizationDto(null, null, "x", List.of()), null))
                .isInstanceOf(IllegalStateException.class);
    }

    private User createNutri() {
        User u = userRepo.save(User.builder()
                .name("N").email("n-" + System.nanoTime() + "@x.com")
                .role(Role.NUTRITIONIST).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").cpf(cpf())
                .build());
        subscriptionService.createFreeSubscription(PlanTarget.NUTRITIONIST, u.getId(), "BRL");
        return u;
    }

    private User createOwner() {
        return userRepo.save(User.builder()
                .name("O").email("o-" + System.nanoTime() + "@x.com")
                .role(Role.COMPANY).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").cpf(cpf())
                .build());
    }

    private String cpf() { return String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)); }
}
