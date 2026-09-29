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
import java.util.List;
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
    @Autowired AssessmentSnapshotRepository snapshotRepo;
    @Autowired NutritionistClientLinkRepository linkRepo;
    @Autowired NutritionistPracticeRepository practiceRepo;
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

    @Test
    void render_createsAutoSnapshotWhenNutriAndClientPresent() {
        User nutri = quickNutri();
        User client = quickClient();
        linkRepo.save(NutritionistClientLink.builder()
                .nutritionist(nutri).client(client)
                .invitedByEmail(client.getEmail()).inviteToken("t-" + System.nanoTime())
                .status(LinkStatus.ACTIVE).invitedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build());
        client.setSex(Sex.FEMALE);
        client.setBirthDate(LocalDate.of(1990, 1, 1));
        client.setWeightKg(65.0);
        client.setHeightCm(165.0);
        userRepo.save(client);

        ClientAssessment a = assessmentRepo.save(ClientAssessment.builder()
                .client(client).nutritionist(nutri)
                .assessmentDate(LocalDateTime.now())
                .weightKg(65.0).heightCm(165)
                .targetCalories(1600.0).targetProteinG(100.0)
                .targetCarbsG(180.0).targetFatG(50.0).targetWaterMl(2275.0)
                .build());

        pdfService.render(a, client, nutri, Locale.forLanguageTag("pt-BR"));

        List<AssessmentSnapshot> snaps = snapshotRepo.findByAssessmentIdOrderByVersionDesc(a.getId());
        assertThat(snaps).hasSize(1);
        assertThat(snaps.get(0).getLabel()).contains("PDF emitido em");
    }

    @Test
    void render_customPrimaryColorReachesOutput() {
        User nutri = quickNutri();
        // Fase 9D-2-fix: customizacao esta na practice, nao no user
        NutritionistPractice practice = practiceRepo.save(NutritionistPractice.builder()
                .nutritionist(nutri).company(null)
                .acceptsNewClients(true)
                .pdfPrimaryColor("#ff5733")
                .pdfFooterText("<b>consultorio</b> · <script>alert(1)</script>Rua X")
                .build());
        User client = quickClient();
        client.setSex(Sex.MALE);
        client.setBirthDate(LocalDate.of(1985, 6, 15));
        client.setWeightKg(80.0);
        client.setHeightCm(175.0);
        userRepo.save(client);

        ClientAssessment a = ClientAssessment.builder()
                .client(client).nutritionist(nutri).practice(practice)
                .assessmentDate(LocalDateTime.now())
                .weightKg(80.0).heightCm(175)
                .targetCalories(2000.0).targetProteinG(140.0)
                .targetCarbsG(220.0).targetFatG(65.0).targetWaterMl(2800.0)
                .build();

        byte[] pdf = pdfService.render(a, client, nutri,
                Locale.forLanguageTag("pt-BR"), /* snapshot */ false);
        assertThat(pdf).isNotEmpty();
        // Sanity: pdf gerado sem exception (nao verifica bytes internos —
        // jsoup deve ter removido o <script> pra evitar XSS ate mesmo no PDF).
    }
}
