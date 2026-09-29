package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.dto.PdfVerificationDto;
import com.prandiumnutri.dto.PrescriptionIssuanceDto;
import com.prandiumnutri.repository.*;
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

/**
 * Fase 9D-3: hash SHA-256 + record + verify.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PrescriptionIssuanceServiceTest {

    @Autowired PrescriptionIssuanceService service;
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
    void sha256_isStableForSameInput() {
        String h1 = PrescriptionIssuanceService.sha256Hex("hello");
        String h2 = PrescriptionIssuanceService.sha256Hex("hello");
        assertThat(h1).isEqualTo(h2).hasSize(64);
        // Valor conhecido do SHA-256 de "hello"
        assertThat(h1).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    void recordAndVerify_roundTrip() {
        User nutri = createNutri();
        User client = createClient();
        ClientAssessment a = assessmentRepo.save(ClientAssessment.builder()
                .client(client).nutritionist(nutri)
                .assessmentDate(LocalDateTime.now())
                .weightKg(70.0).heightCm(170)
                .build());

        byte[] pdf = "fake-pdf-bytes-1".getBytes();
        PrescriptionIssuance rec = service.record(a, client, nutri, null, pdf, "NUTRITIONIST");
        assertThat(rec.getPdfSha256()).hasSize(64);
        assertThat(rec.getPdfSizeBytes()).isEqualTo(pdf.length);

        PdfVerificationDto ok = service.verify(rec.getPdfSha256());
        assertThat(ok.valid()).isTrue();
        assertThat(ok.nutriName()).isEqualTo(nutri.getName());

        PdfVerificationDto bad = service.verify("0".repeat(64));
        assertThat(bad.valid()).isFalse();

        List<PrescriptionIssuanceDto> forClient = service.listForClient(client.getId());
        assertThat(forClient).hasSize(1);
        assertThat(forClient.get(0).requesterRole()).isEqualTo("NUTRITIONIST");
    }

    @Test
    void verify_shortHashIsInvalid() {
        assertThat(service.verify("abc").valid()).isFalse();
        assertThat(service.verify(null).valid()).isFalse();
    }

    private User createNutri() {
        User u = userRepo.save(User.builder()
                .name("Dra Test").email("n-" + System.nanoTime() + "@x.com")
                .role(Role.NUTRITIONIST).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").cpf(cpf()).crn("CRN-99999")
                .build());
        subscriptionService.createFreeSubscription(PlanTarget.NUTRITIONIST, u.getId(), "BRL");
        return u;
    }

    private User createClient() {
        return userRepo.save(User.builder()
                .name("Cliente Teste").email("c-" + System.nanoTime() + "@x.com")
                .role(Role.USER).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").cpf(cpf())
                .build());
    }

    private String cpf() { return String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)); }
}
