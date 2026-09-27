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
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Smoke test do gerador de PDF. Nao valida conteudo binario — apenas garante que
 * o pipeline Thymeleaf → OpenHTMLtoPDF roda sem exception e produz bytes com
 * assinatura de PDF (%PDF).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PrescriptionPdfServiceTest {

    @Autowired PrescriptionPdfService pdfService;
    @Autowired UserRepository userRepo;
    @Autowired ClientAssessmentRepository assessmentRepo;
    @Autowired SubscriptionService subscriptionService;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void render_producesPdfBytes() {
        User nutri = userRepo.save(User.builder()
                .name("Dra. Camila").email("cami-" + System.nanoTime() + "@x.com")
                .role(Role.NUTRITIONIST).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").cpf(cpf()).crn("CRN-12345")
                .build());
        subscriptionService.createFreeSubscription(PlanTarget.NUTRITIONIST, nutri.getId(), "BRL");

        User client = userRepo.save(User.builder()
                .name("João da Silva").email("j-" + System.nanoTime() + "@x.com")
                .role(Role.USER).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").cpf(cpf())
                .sex(Sex.MALE).birthDate(LocalDate.of(1994, 5, 20))
                .weightKg(80.0).heightCm(180.0)
                .build());

        ClientAssessment a = assessmentRepo.save(ClientAssessment.builder()
                .client(client).nutritionist(nutri)
                .assessmentDate(LocalDateTime.now())
                .weightKg(80.0).heightCm(180)
                .tmbFormula("MIFFLIN")
                .activityFactor(1.55)
                .calorieDeltaPct(-0.20)
                .proteinGPerKg(1.8)
                .fatPct(0.28)
                .targetCalories(2200.0)
                .targetProteinG(144.0)
                .targetCarbsG(240.0)
                .targetFatG(70.0)
                .targetWaterMl(2800.0)
                .prescribedAt(LocalDateTime.now())
                .notes("Beber agua ao longo do dia. Priorizar proteina magra.")
                .build());

        byte[] pdf = pdfService.render(a, client, nutri, new Locale("pt", "BR"));

        assertThat(pdf).isNotEmpty();
        // Header PDF: assinatura magica "%PDF-"
        String head = new String(pdf, 0, Math.min(5, pdf.length));
        assertThat(head).isEqualTo("%PDF-");
    }

    @Test
    void render_failsWhenNoPrescription() {
        User nutri = quickNutri();
        User client = quickClient();
        ClientAssessment a = assessmentRepo.save(ClientAssessment.builder()
                .client(client).nutritionist(nutri)
                .assessmentDate(LocalDateTime.now())
                .weightKg(70.0)
                // sem targetCalories
                .build());

        assertThatThrownBy(() -> pdfService.render(a, client, nutri, Locale.forLanguageTag("pt-BR")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("targetCalories");
    }

    private User quickNutri() {
        User u = userRepo.save(User.builder()
                .name("N").email("n-" + System.nanoTime() + "@x.com")
                .role(Role.NUTRITIONIST).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").cpf(cpf())
                .build());
        subscriptionService.createFreeSubscription(PlanTarget.NUTRITIONIST, u.getId(), "BRL");
        return u;
    }

    private User quickClient() {
        return userRepo.save(User.builder()
                .name("C").email("c-" + System.nanoTime() + "@x.com")
                .role(Role.USER).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").cpf(cpf())
                .build());
    }

    private String cpf() {
        return String.format("%011d", Math.abs(System.nanoTime() % 100000000000L));
    }
}
